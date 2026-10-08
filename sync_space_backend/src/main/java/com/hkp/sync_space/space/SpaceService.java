package com.hkp.sync_space.space;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hkp.sync_space.auth.AuthUser;
import com.hkp.sync_space.board.BoardChanged;
import com.hkp.sync_space.board.BoardColumn;
import com.hkp.sync_space.board.BoardColumnRepository;
import com.hkp.sync_space.board.BoardEvents;
import com.hkp.sync_space.board.BoardResponse;
import com.hkp.sync_space.board.BoardResponses;
import com.hkp.sync_space.board.BoardService;
import com.hkp.sync_space.board.BoardState;
import com.hkp.sync_space.board.TaskRepository;
import com.hkp.sync_space.common.ApiException;
import com.hkp.sync_space.user.User;
import com.hkp.sync_space.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class SpaceService {

	private final SpaceRepository spaceRepository;
	private final SpaceMemberRepository memberRepository;
	private final BoardColumnRepository columnRepository;
	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final SpaceAccess spaceAccess;
	private final BoardService boardService;
	private final ApplicationEventPublisher events;

	public SpaceService(SpaceRepository spaceRepository, SpaceMemberRepository memberRepository,
			BoardColumnRepository columnRepository, TaskRepository taskRepository, UserRepository userRepository,
			SpaceAccess spaceAccess, BoardService boardService, ApplicationEventPublisher events) {
		this.spaceRepository = spaceRepository;
		this.memberRepository = memberRepository;
		this.columnRepository = columnRepository;
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
		this.spaceAccess = spaceAccess;
		this.boardService = boardService;
		this.events = events;
	}

	public List<SpaceSummary> list(AuthUser actor) {
		List<SpaceMember> memberships = memberRepository.findByUserId(actor.id());
		if (memberships.isEmpty()) {
			return List.of();
		}
		List<UUID> spaceIds = memberships.stream().map(membership -> membership.getSpace().getId()).toList();
		Map<UUID, Integer> counts = memberRepository.countBySpaceIds(spaceIds).stream()
				.collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).intValue()));
		return memberships.stream()
				.sorted(Comparator.comparing(membership -> membership.getSpace().getName(), String.CASE_INSENSITIVE_ORDER))
				.map(membership -> new SpaceSummary(membership.getSpace().getId(), membership.getSpace().getName(),
						membership.getRole().name(), counts.getOrDefault(membership.getSpace().getId(), 1)))
				.toList();
	}

	@Transactional
	public SpaceSummary create(AuthUser actor, CreateSpaceRequest request) {
		User user = userRepository.findById(actor.id())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
		Space space = spaceRepository.save(new Space(request.name().trim(), user));
		memberRepository.save(new SpaceMember(space, user, MemberRole.OWNER));
		columnRepository.save(new BoardColumn(space, "To Do", 0));
		columnRepository.save(new BoardColumn(space, "In Progress", 1));
		columnRepository.save(new BoardColumn(space, "Done", 2));
		return new SpaceSummary(space.getId(), space.getName(), MemberRole.OWNER.name(), 1);
	}

	@Transactional
	public BoardResponse rename(UUID spaceId, AuthUser actor, RenameSpaceRequest request) {
		SpaceMember owner = spaceAccess.lockOwner(spaceId, actor.id());
		owner.getSpace().setName(request.name().trim());
		return touchAndPublish(owner, BoardEvents.SPACE_UPDATED);
	}

	@Transactional
	public void delete(UUID spaceId, AuthUser actor) {
		SpaceMember owner = spaceAccess.lockOwner(spaceId, actor.id());
		Space space = owner.getSpace();
		events.publishEvent(new BoardChanged(BoardEvents.SPACE_DELETED, spaceId,
				new BoardState(spaceId, space.getName(), space.getRevision() + 1, List.of(), List.of())));
		taskRepository.deleteAll(taskRepository.findBySpaceId(spaceId));
		columnRepository.deleteAll(columnRepository.findBySpaceIdOrderByPositionAsc(spaceId));
		memberRepository.deleteAll(memberRepository.findBySpaceId(spaceId));
		spaceRepository.delete(space);
	}

	public List<com.hkp.sync_space.board.MemberResponse> members(UUID spaceId, AuthUser actor) {
		spaceAccess.requireMember(spaceId, actor.id());
		return boardService.loadState(spaceId).members();
	}

	@Transactional
	public BoardResponse invite(UUID spaceId, AuthUser actor, InviteMemberRequest request) {
		SpaceMember owner = spaceAccess.lockOwner(spaceId, actor.id());
		String email = request.email().trim().toLowerCase();
		User invitee = userRepository.findByEmail(email)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No account exists for that email"));
		if (memberRepository.existsBySpaceIdAndUserId(spaceId, invitee.getId())) {
			throw new ApiException(HttpStatus.CONFLICT, "That person is already in this space");
		}
		memberRepository.save(new SpaceMember(owner.getSpace(), invitee, MemberRole.MEMBER));
		return touchAndPublish(owner, BoardEvents.MEMBERS_CHANGED);
	}

	@Transactional
	public BoardResponse removeMember(UUID spaceId, UUID userId, AuthUser actor) {
		SpaceMember owner = spaceAccess.lockOwner(spaceId, actor.id());
		SpaceMember target = memberRepository.findBySpaceIdAndUserId(spaceId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That person is not in this space"));
		if (target.getRole() == MemberRole.OWNER) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "The owner cannot be removed");
		}
		User actorUser = userRepository.findById(actor.id())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
		boardService.clearAssignee(owner.getSpace(), actorUser, userId);
		memberRepository.delete(target);
		return touchAndPublish(owner, BoardEvents.MEMBERS_CHANGED);
	}

	private BoardResponse touchAndPublish(SpaceMember member, String type) {
		Space space = member.getSpace();
		space.setRevision(space.getRevision() + 1);
		BoardState state = boardService.loadState(space.getId());
		events.publishEvent(new BoardChanged(type, space.getId(), state));
		return BoardResponses.from(state, member.getRole());
	}

}
