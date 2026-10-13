package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hkp.sync_space.auth.CurrentUser;

@RestController
@RequestMapping("/api/me")
public class WorkController {

	private final WorkService workService;
	private final NotificationService notificationService;

	public WorkController(WorkService workService, NotificationService notificationService) {
		this.workService = workService;
		this.notificationService = notificationService;
	}

	@GetMapping("/tasks")
	List<AssignedTaskResponse> tasks() {
		return workService.assignedToMe(CurrentUser.get());
	}

	@GetMapping("/notifications")
	List<NotificationResponse> notifications() {
		return notificationService.recent(CurrentUser.get());
	}

	@PostMapping("/notifications/{notificationId}/read")
	NotificationResponse markRead(@PathVariable UUID notificationId) {
		return notificationService.markRead(notificationId, CurrentUser.get());
	}

	@PostMapping("/notifications/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markAllRead() {
		notificationService.markAllRead(CurrentUser.get());
	}

}
