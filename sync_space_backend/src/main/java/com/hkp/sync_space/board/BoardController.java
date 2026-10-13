package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hkp.sync_space.auth.CurrentUser;

@RestController
@RequestMapping("/api/spaces/{spaceId}")
public class BoardController {

	private final BoardService boardService;

	public BoardController(BoardService boardService) {
		this.boardService = boardService;
	}

	@GetMapping("/board")
	BoardResponse board(@PathVariable UUID spaceId) {
		return boardService.board(spaceId, CurrentUser.get());
	}

	@PostMapping("/columns")
	BoardResponse createColumn(@PathVariable UUID spaceId, @Valid @RequestBody CreateColumnRequest request) {
		return boardService.createColumn(spaceId, CurrentUser.get(), request);
	}

	@PatchMapping("/columns/{columnId}")
	BoardResponse renameColumn(@PathVariable UUID spaceId, @PathVariable UUID columnId,
			@Valid @RequestBody RenameColumnRequest request) {
		return boardService.renameColumn(spaceId, columnId, CurrentUser.get(), request);
	}

	@PostMapping("/columns/reorder")
	BoardResponse reorderColumns(@PathVariable UUID spaceId, @Valid @RequestBody ReorderColumnsRequest request) {
		return boardService.reorderColumns(spaceId, CurrentUser.get(), request);
	}

	@DeleteMapping("/columns/{columnId}")
	BoardResponse deleteColumn(@PathVariable UUID spaceId, @PathVariable UUID columnId) {
		return boardService.deleteColumn(spaceId, columnId, CurrentUser.get());
	}

	@PostMapping("/tasks")
	BoardResponse createTask(@PathVariable UUID spaceId, @Valid @RequestBody CreateTaskRequest request) {
		return boardService.createTask(spaceId, CurrentUser.get(), request);
	}

	@PatchMapping("/tasks/{taskId}")
	BoardResponse updateTask(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody UpdateTaskRequest request) {
		return boardService.updateTask(spaceId, taskId, CurrentUser.get(), request);
	}

	@PatchMapping("/tasks/{taskId}/assignees")
	BoardResponse assign(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody AssignTaskRequest request) {
		return boardService.assign(spaceId, taskId, CurrentUser.get(), request);
	}

	@PatchMapping("/tasks/{taskId}/labels")
	BoardResponse setLabels(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody SetTaskLabelsRequest request) {
		return boardService.setLabels(spaceId, taskId, CurrentUser.get(), request);
	}

	@PatchMapping("/tasks/{taskId}/priority")
	BoardResponse setPriority(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody SetTaskPriorityRequest request) {
		return boardService.setPriority(spaceId, taskId, CurrentUser.get(), request);
	}

	@PatchMapping("/tasks/{taskId}/due")
	BoardResponse setDue(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody SetTaskDueRequest request) {
		return boardService.setDue(spaceId, taskId, CurrentUser.get(), request);
	}

	@GetMapping("/archive")
	List<TaskResponse> archived(@PathVariable UUID spaceId) {
		return boardService.archived(spaceId, CurrentUser.get());
	}

	@PostMapping("/tasks/{taskId}/archive")
	BoardResponse archiveTask(@PathVariable UUID spaceId, @PathVariable UUID taskId) {
		return boardService.archiveTask(spaceId, taskId, CurrentUser.get());
	}

	@PostMapping("/tasks/{taskId}/restore")
	BoardResponse restoreTask(@PathVariable UUID spaceId, @PathVariable UUID taskId) {
		return boardService.restoreTask(spaceId, taskId, CurrentUser.get());
	}

	@DeleteMapping("/tasks/{taskId}")
	BoardResponse deleteTask(@PathVariable UUID spaceId, @PathVariable UUID taskId) {
		return boardService.deleteTask(spaceId, taskId, CurrentUser.get());
	}

	@PostMapping("/tasks/{taskId}/move")
	BoardResponse moveTask(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody MoveTaskRequest request) {
		return boardService.moveTask(spaceId, taskId, CurrentUser.get(), request);
	}

	@GetMapping("/tasks/{taskId}/activity")
	List<TaskActivityResponse> activity(@PathVariable UUID spaceId, @PathVariable UUID taskId) {
		return boardService.activity(spaceId, taskId, CurrentUser.get());
	}

	@PostMapping("/tasks/{taskId}/checks")
	BoardResponse addCheck(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody CreateCheckRequest request) {
		return boardService.addCheck(spaceId, taskId, CurrentUser.get(), request);
	}

	@PatchMapping("/tasks/{taskId}/checks/{checkId}")
	BoardResponse updateCheck(@PathVariable UUID spaceId, @PathVariable UUID taskId, @PathVariable UUID checkId,
			@Valid @RequestBody UpdateCheckRequest request) {
		return boardService.updateCheck(spaceId, taskId, checkId, CurrentUser.get(), request);
	}

	@DeleteMapping("/tasks/{taskId}/checks/{checkId}")
	BoardResponse deleteCheck(@PathVariable UUID spaceId, @PathVariable UUID taskId, @PathVariable UUID checkId) {
		return boardService.deleteCheck(spaceId, taskId, checkId, CurrentUser.get());
	}

	@PostMapping("/tasks/{taskId}/comments")
	List<TaskActivityResponse> comment(@PathVariable UUID spaceId, @PathVariable UUID taskId,
			@Valid @RequestBody CreateCommentRequest request) {
		return boardService.addComment(spaceId, taskId, CurrentUser.get(), request);
	}

}
