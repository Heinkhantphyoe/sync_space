package com.hkp.sync_space.board;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hkp.sync_space.auth.AuthUser;

@Service
@Transactional(readOnly = true)
public class WorkService {

	private final TaskRepository taskRepository;

	public WorkService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	public List<AssignedTaskResponse> assignedToMe(AuthUser actor) {
		return taskRepository.findAssignedToUser(actor.id()).stream()
				.sorted(Comparator
						.comparing((Task task) -> task.getDueOn(), Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(task -> task.getSpace().getName(), String.CASE_INSENSITIVE_ORDER)
						.thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER))
				.map(task -> new AssignedTaskResponse(task.getId(), task.getTitle(), task.getPriority(), task.getDueOn(),
						task.getSpace().getId(), task.getSpace().getName(), task.getColumn().getName()))
				.toList();
	}

}
