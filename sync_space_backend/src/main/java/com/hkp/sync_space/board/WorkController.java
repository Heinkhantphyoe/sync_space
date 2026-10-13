package com.hkp.sync_space.board;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hkp.sync_space.auth.CurrentUser;

@RestController
@RequestMapping("/api/me")
public class WorkController {

	private final WorkService workService;

	public WorkController(WorkService workService) {
		this.workService = workService;
	}

	@GetMapping("/tasks")
	List<AssignedTaskResponse> tasks() {
		return workService.assignedToMe(CurrentUser.get());
	}

}
