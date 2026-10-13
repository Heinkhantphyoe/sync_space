package com.hkp.sync_space.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCheckRepository extends JpaRepository<TaskCheck, UUID> {

	@Query("""
			select checkItem from TaskCheck checkItem
			join fetch checkItem.task
			where checkItem.task.space.id = :spaceId
			order by checkItem.position asc
			""")
	List<TaskCheck> findBySpaceId(@Param("spaceId") UUID spaceId);

	List<TaskCheck> findByTaskIdOrderByPositionAsc(UUID taskId);

	Optional<TaskCheck> findByIdAndTaskId(UUID id, UUID taskId);

}
