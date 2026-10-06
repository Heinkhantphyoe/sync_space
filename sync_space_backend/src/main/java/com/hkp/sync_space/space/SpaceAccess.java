package com.hkp.sync_space.space;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.hkp.sync_space.common.ApiException;

@Service
public class SpaceAccess {

	private final SpaceRepository spaceRepository;
	private final SpaceMemberRepository memberRepository;

	public SpaceAccess(SpaceRepository spaceRepository, SpaceMemberRepository memberRepository) {
		this.spaceRepository = spaceRepository;
		this.memberRepository = memberRepository;
	}

	public SpaceMember requireMember(UUID spaceId, UUID userId) {
		return memberRepository.findBySpaceIdAndUserId(spaceId, userId).orElseThrow(this::notMember);
	}

	public SpaceMember lockMember(UUID spaceId, UUID userId) {
		if (!memberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
			throw notMember();
		}
		spaceRepository.lockById(spaceId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Space not found"));
		return memberRepository.findBySpaceIdAndUserId(spaceId, userId).orElseThrow(this::notMember);
	}

	public SpaceMember lockOwner(UUID spaceId, UUID userId) {
		SpaceMember member = lockMember(spaceId, userId);
		if (member.getRole() != MemberRole.OWNER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Only the owner can do that");
		}
		return member;
	}

	private ApiException notMember() {
		return new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this space");
	}

}
