package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID> {

	@Query("""
			select activity from TaskActivity activity
			join fetch activity.actor
			where activity.space.id = :spaceId and activity.task.id = :taskId
			order by activity.createdAt asc, activity.id asc
			""")
	List<TaskActivity> findForTask(@Param("spaceId") UUID spaceId, @Param("taskId") UUID taskId);

}
