package com.hkp.sync_space.space;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hkp.sync_space.auth.CurrentUser;
import com.hkp.sync_space.board.BoardResponse;
import com.hkp.sync_space.board.MemberResponse;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

	private final SpaceService spaceService;

	public SpaceController(SpaceService spaceService) {
		this.spaceService = spaceService;
	}

	@GetMapping
	List<SpaceSummary> list() {
		return spaceService.list(CurrentUser.get());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	SpaceSummary create(@Valid @RequestBody CreateSpaceRequest request) {
		return spaceService.create(CurrentUser.get(), request);
	}

	@PatchMapping("/{spaceId}")
	BoardResponse rename(@PathVariable UUID spaceId, @Valid @RequestBody RenameSpaceRequest request) {
		return spaceService.rename(spaceId, CurrentUser.get(), request);
	}

	@DeleteMapping("/{spaceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID spaceId) {
		spaceService.delete(spaceId, CurrentUser.get());
	}

	@GetMapping("/{spaceId}/members")
	List<MemberResponse> members(@PathVariable UUID spaceId) {
		return spaceService.members(spaceId, CurrentUser.get());
	}

	@PostMapping("/{spaceId}/members")
	BoardResponse invite(@PathVariable UUID spaceId, @Valid @RequestBody InviteMemberRequest request) {
		return spaceService.invite(spaceId, CurrentUser.get(), request);
	}

	@DeleteMapping("/{spaceId}/members/{userId}")
	BoardResponse removeMember(@PathVariable UUID spaceId, @PathVariable UUID userId) {
		return spaceService.removeMember(spaceId, userId, CurrentUser.get());
	}

}
