package com.hkp.sync_space.space;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, UUID> {

	@Query("select m from SpaceMember m join fetch m.space where m.user.id = :userId")
	List<SpaceMember> findByUserId(@Param("userId") UUID userId);

	@Query("select m from SpaceMember m join fetch m.user where m.space.id = :spaceId")
	List<SpaceMember> findBySpaceId(@Param("spaceId") UUID spaceId);

	Optional<SpaceMember> findBySpaceIdAndUserId(UUID spaceId, UUID userId);

	boolean existsBySpaceIdAndUserId(UUID spaceId, UUID userId);

	@Query("select m.space.id, count(m) from SpaceMember m where m.space.id in :spaceIds group by m.space.id")
	List<Object[]> countBySpaceIds(@Param("spaceIds") List<UUID> spaceIds);

	@Modifying
	@Query("delete from SpaceMember m where m.space.id = :spaceId")
	void deleteBySpaceId(@Param("spaceId") UUID spaceId);

}
