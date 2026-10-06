package com.hkp.sync_space.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

	List<BoardColumn> findBySpaceIdOrderByPositionAsc(UUID spaceId);

	Optional<BoardColumn> findByIdAndSpaceId(UUID id, UUID spaceId);

	@Modifying
	@Query("delete from BoardColumn c where c.space.id = :spaceId")
	void deleteBySpaceId(@Param("spaceId") UUID spaceId);

}
