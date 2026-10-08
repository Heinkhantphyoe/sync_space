package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hkp.sync_space.auth.AuthUser;
import com.hkp.sync_space.common.ApiException;
import com.hkp.sync_space.space.MemberRole;
import com.hkp.sync_space.space.Space;
import com.hkp.sync_space.space.SpaceAccess;
import com.hkp.sync_space.space.SpaceMember;
import com.hkp.sync_space.space.SpaceMemberRepository;
import com.hkp.sync_space.space.SpaceRepository;
import com.hkp.sync_space.user.User;
import com.hkp.sync_space.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class BoardService {

	private final SpaceRepository spaceRepository;
	private final SpaceMemberRepository memberRepository;
	private final BoardColumnRepository columnRepository;
	private final TaskRepository taskRepository;
	private final TaskActivityRepository activityRepository;
	private final UserRepository userRepository;
	private final SpaceAccess spaceAccess;
	private final ApplicationEventPublisher events;

	public BoardService(SpaceRepository spaceRepository, SpaceMemberRepository memberRepository,
			BoardColumnRepository columnRepository, TaskRepository taskRepository,
			TaskActivityRepository activityRepository, UserRepository userRepository, SpaceAccess spaceAccess,
			ApplicationEventPublisher events) {
		this.spaceRepository = spaceRepository;
		this.memberRepository = memberRepository;
		this.columnRepository = columnRepository;
		this.taskRepository = taskRepository;
		this.activityRepository = activityRepository;
		this.userRepository = userRepository;
		this.spaceAccess = spaceAccess;
		this.events = events;
	}

	public BoardResponse board(UUID spaceId, AuthUser actor) {
		SpaceMember member = spaceAccess.requireMember(spaceId, actor.id());
		return BoardResponses.from(loadState(spaceId), member.getRole());
	}

	public BoardState loadState(UUID spaceId) {
		Space space = spaceRepository.findById(spaceId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Space not found"));
		Map<UUID, List<Task>> tasksByColumn = taskRepository.findBySpaceId(spaceId).stream()
				.collect(Collectors.groupingBy(task -> task.getColumn().getId()));
		List<ColumnResponse> columns = columnRepository.findBySpaceIdOrderByPositionAsc(spaceId).stream()
				.map(column -> new ColumnResponse(column.getId(), column.getName(), column.getPosition(),
						tasksByColumn.getOrDefault(column.getId(), List.of()).stream()
								.sorted(Comparator.comparingInt(Task::getPosition))
								.map(this::toTask)
								.toList()))
				.toList();
		List<MemberResponse> members = memberRepository.findBySpaceId(spaceId).stream()
				.sorted(Comparator
						.comparingInt((SpaceMember member) -> member.getRole() == MemberRole.OWNER ? 0 : 1)
						.thenComparing(member -> member.getUser().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
				.map(member -> new MemberResponse(member.getUser().getId(), member.getUser().getDisplayName(),
						member.getUser().getEmail(), member.getRole().name()))
				.toList();
		return new BoardState(space.getId(), space.getName(), space.getRevision(), columns, members);
	}

	@Transactional
	public BoardResponse createColumn(UUID spaceId, AuthUser actor, CreateColumnRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		int position = columnRepository.findBySpaceIdOrderByPositionAsc(spaceId).size();
		columnRepository.save(new BoardColumn(member.getSpace(), request.name().trim(), position));
		return touchAndPublish(member, BoardEvents.COLUMN_CREATED);
	}

	@Transactional
	public BoardResponse renameColumn(UUID spaceId, UUID columnId, AuthUser actor, RenameColumnRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		BoardColumn column = column(spaceId, columnId);
		column.setName(request.name().trim());
		return touchAndPublish(member, BoardEvents.COLUMN_UPDATED);
	}

	@Transactional
	public BoardResponse reorderColumns(UUID spaceId, AuthUser actor, ReorderColumnsRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		List<BoardColumn> columns = columnRepository.findBySpaceIdOrderByPositionAsc(spaceId);
		if (request.columnIds().size() != columns.size()
				|| !new HashSet<>(request.columnIds()).equals(columns.stream().map(BoardColumn::getId).collect(Collectors.toSet()))
				|| new HashSet<>(request.columnIds()).size() != request.columnIds().size()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Column order must include every column once");
		}
		Map<UUID, BoardColumn> byId = columns.stream().collect(Collectors.toMap(BoardColumn::getId, column -> column));
		for (int index = 0; index < request.columnIds().size(); index++) {
			byId.get(request.columnIds().get(index)).setPosition(index);
		}
		return touchAndPublish(member, BoardEvents.COLUMN_REORDERED);
	}

	@Transactional
	public BoardResponse deleteColumn(UUID spaceId, UUID columnId, AuthUser actor) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		BoardColumn column = column(spaceId, columnId);
		taskRepository.deleteAll(taskRepository.findByColumnIdOrderByPositionAsc(columnId));
		columnRepository.delete(column);
		List<BoardColumn> remaining = columnRepository.findBySpaceIdOrderByPositionAsc(spaceId).stream()
				.filter(existing -> !existing.getId().equals(columnId))
				.toList();
		for (int index = 0; index < remaining.size(); index++) {
			remaining.get(index).setPosition(index);
		}
		return touchAndPublish(member, BoardEvents.COLUMN_DELETED);
	}

	@Transactional
	public BoardResponse createTask(UUID spaceId, AuthUser actor, CreateTaskRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		BoardColumn column = column(spaceId, request.columnId());
		User creator = userRepository.findById(actor.id())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
		int position = taskRepository.findByColumnIdOrderByPositionAsc(column.getId()).size();
		String description = blankToNull(request.description());
		Task created = taskRepository.save(
				new Task(member.getSpace(), column, request.title().trim(), description, position, creator));
		record(member.getSpace(), created, creator, TaskActivityKind.CREATED, null, Instant.now());
		return touchAndPublish(member, BoardEvents.TASK_CREATED);
	}

	@Transactional
	public BoardResponse updateTask(UUID spaceId, UUID taskId, AuthUser actor, UpdateTaskRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		if (request.title() == null && request.description() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Nothing to update");
		}
		Task task = task(spaceId, taskId);
		User actorUser = user(actor.id());
		Instant stamp = Instant.now();
		if (request.title() != null) {
			String title = request.title().trim();
			if (title.isEmpty()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required");
			}
			if (!title.equals(task.getTitle())) {
				task.setTitle(title);
				record(member.getSpace(), task, actorUser, TaskActivityKind.RENAMED, title, stamp);
				stamp = stamp.plusMillis(1);
			}
		}
		if (request.description() != null) {
			String description = blankToNull(request.description());
			if (!Objects.equals(description, task.getDescription())) {
				task.setDescription(description);
				record(member.getSpace(), task, actorUser, TaskActivityKind.DESCRIPTION_CHANGED, null, stamp);
			}
		}
		task.setUpdatedAt(Instant.now());
		return touchAndPublish(member, BoardEvents.TASK_UPDATED);
	}

	@Transactional
	public BoardResponse deleteTask(UUID spaceId, UUID taskId, AuthUser actor) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		Task task = task(spaceId, taskId);
		UUID columnId = task.getColumn().getId();
		taskRepository.delete(task);
		List<Task> remaining = taskRepository.findByColumnIdOrderByPositionAsc(columnId).stream()
				.filter(existing -> !existing.getId().equals(taskId))
				.toList();
		rewrite(remaining);
		return touchAndPublish(member, BoardEvents.TASK_DELETED);
	}

	@Transactional
	public BoardResponse moveTask(UUID spaceId, UUID taskId, AuthUser actor, MoveTaskRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		Task task = task(spaceId, taskId);
		BoardColumn target = column(spaceId, request.toColumnId());
		UUID fromColumnId = task.getColumn().getId();
		List<Task> fromTasks = new ArrayList<>(taskRepository.findByColumnIdOrderByPositionAsc(fromColumnId));
		fromTasks.removeIf(existing -> existing.getId().equals(taskId));
		if (fromColumnId.equals(target.getId())) {
			fromTasks.add(clamp(request.toIndex(), fromTasks.size()), task);
			rewrite(fromTasks);
		}
		else {
			rewrite(fromTasks);
			List<Task> toTasks = new ArrayList<>(taskRepository.findByColumnIdOrderByPositionAsc(target.getId()));
			task.setColumn(target);
			toTasks.add(clamp(request.toIndex(), toTasks.size()), task);
			rewrite(toTasks);
		}
		if (!fromColumnId.equals(target.getId())) {
			record(member.getSpace(), task, user(actor.id()), TaskActivityKind.MOVED, target.getName(), Instant.now());
		}
		task.setUpdatedAt(Instant.now());
		return touchAndPublish(member, BoardEvents.TASK_MOVED);
	}

	@Transactional
	public BoardResponse assign(UUID spaceId, UUID taskId, AuthUser actor, AssignTaskRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		Task task = task(spaceId, taskId);
		LinkedHashSet<UUID> requested = new LinkedHashSet<>();
		for (UUID userId : request.userIds()) {
			if (userId == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Only people in this space can be assigned");
			}
			requested.add(userId);
		}
		Set<UUID> memberIds = memberRepository.findBySpaceId(spaceId).stream()
				.map(spaceMember -> spaceMember.getUser().getId())
				.collect(Collectors.toSet());
		if (!memberIds.containsAll(requested)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Only people in this space can be assigned");
		}
		Set<UUID> currentIds = task.getAssignees().stream().map(User::getId).collect(Collectors.toSet());
		if (currentIds.equals(requested)) {
			return BoardResponses.from(loadState(spaceId), member.getRole());
		}
		Map<UUID, User> usersById = userRepository.findAllById(requested).stream()
				.collect(Collectors.toMap(User::getId, user -> user));
		User actorUser = user(actor.id());
		Instant stamp = Instant.now();
		List<User> removed = task.getAssignees().stream()
				.filter(assignee -> !requested.contains(assignee.getId()))
				.toList();
		for (User assignee : removed) {
			task.getAssignees().remove(assignee);
			record(member.getSpace(), task, actorUser, TaskActivityKind.UNASSIGNED, assignee.getDisplayName(), stamp);
			stamp = stamp.plusMillis(1);
		}
		for (UUID userId : requested) {
			if (!currentIds.contains(userId)) {
				User assignee = usersById.get(userId);
				task.getAssignees().add(assignee);
				record(member.getSpace(), task, actorUser, TaskActivityKind.ASSIGNED, assignee.getDisplayName(), stamp);
				stamp = stamp.plusMillis(1);
			}
		}
		task.setUpdatedAt(Instant.now());
		return touchAndPublish(member, BoardEvents.TASK_UPDATED);
	}

	@Transactional
	public void clearAssignee(Space space, User actor, UUID userId) {
		List<Task> tasks = taskRepository.findAssignedTo(space.getId(), userId);
		if (tasks.isEmpty()) {
			return;
		}
		String name = tasks.getFirst().getAssignees().stream()
				.filter(assignee -> assignee.getId().equals(userId))
				.map(User::getDisplayName)
				.findFirst()
				.orElse("someone");
		Instant stamp = Instant.now();
		for (Task task : tasks) {
			task.getAssignees().removeIf(assignee -> assignee.getId().equals(userId));
			task.setUpdatedAt(stamp);
			record(space, task, actor, TaskActivityKind.UNASSIGNED, name, stamp);
			stamp = stamp.plusMillis(1);
		}
	}

	public List<TaskActivityResponse> activity(UUID spaceId, UUID taskId, AuthUser actor) {
		spaceAccess.requireMember(spaceId, actor.id());
		task(spaceId, taskId);
		return activityRepository.findForTask(spaceId, taskId).stream().map(this::toActivity).toList();
	}

	@Transactional
	public List<TaskActivityResponse> addComment(UUID spaceId, UUID taskId, AuthUser actor, CreateCommentRequest request) {
		SpaceMember member = spaceAccess.lockMember(spaceId, actor.id());
		Task task = task(spaceId, taskId);
		String body = request.body().trim();
		if (body.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Comment is required");
		}
		record(member.getSpace(), task, user(actor.id()), TaskActivityKind.COMMENT, body, Instant.now());
		touchAndPublish(member, BoardEvents.TASK_COMMENTED);
		return activityRepository.findForTask(spaceId, taskId).stream().map(this::toActivity).toList();
	}

	private BoardResponse touchAndPublish(SpaceMember member, String type) {
		Space space = member.getSpace();
		space.setRevision(space.getRevision() + 1);
		BoardState state = loadState(space.getId());
		events.publishEvent(new BoardChanged(type, space.getId(), state));
		return BoardResponses.from(state, member.getRole());
	}

	private BoardColumn column(UUID spaceId, UUID columnId) {
		return columnRepository.findByIdAndSpaceId(columnId, spaceId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Column not found"));
	}

	private Task task(UUID spaceId, UUID taskId) {
		return taskRepository.findByIdAndSpaceId(taskId, spaceId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
	}

	private void record(Space space, Task task, User actor, TaskActivityKind kind, String body, Instant createdAt) {
		activityRepository.save(new TaskActivity(space, task, actor, kind, body, createdAt));
	}

	private User user(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
	}

	private TaskActivityResponse toActivity(TaskActivity entry) {
		String summary = switch (entry.getKind()) {
			case CREATED -> "created this task";
			case MOVED -> "moved this to " + entry.getBody();
			case RENAMED -> "renamed this to " + entry.getBody();
			case DESCRIPTION_CHANGED -> "updated the description";
			case COMMENT -> null;
			case ASSIGNED -> "assigned " + entry.getBody();
			case UNASSIGNED -> "unassigned " + entry.getBody();
		};
		String body = entry.getKind() == TaskActivityKind.COMMENT ? entry.getBody() : null;
		return new TaskActivityResponse(entry.getId(), entry.getKind().name(), entry.getActor().getId(),
				entry.getActor().getDisplayName(), summary, body, entry.getCreatedAt());
	}

	private TaskResponse toTask(Task task) {
		List<AssigneeResponse> assignees = task.getAssignees().stream()
				.sorted(Comparator.comparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER))
				.map(assignee -> new AssigneeResponse(assignee.getId(), assignee.getDisplayName()))
				.toList();
		return new TaskResponse(task.getId(), task.getColumn().getId(), task.getTitle(), task.getDescription(),
				task.getPosition(), task.getCreatedBy().getId(), task.getUpdatedAt(), assignees);
	}

	private static void rewrite(List<Task> tasks) {
		for (int index = 0; index < tasks.size(); index++) {
			tasks.get(index).setPosition(index);
		}
	}

	private static int clamp(int index, int size) {
		return Math.min(Math.max(index, 0), size);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
