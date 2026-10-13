package com.hkp.sync_space.space;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceInviteRepository extends JpaRepository<SpaceInvite, UUID> {

	@Query("select invite from SpaceInvite invite join fetch invite.space where invite.email = :email")
	List<SpaceInvite> findByEmail(@Param("email") String email);

	List<SpaceInvite> findBySpaceIdOrderByCreatedAtAsc(UUID spaceId);

	Optional<SpaceInvite> findByIdAndSpaceId(UUID id, UUID spaceId);

	boolean existsBySpaceIdAndEmail(UUID spaceId, String email);

}
