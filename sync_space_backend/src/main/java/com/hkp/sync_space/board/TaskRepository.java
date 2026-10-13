package com.hkp.sync_space.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	@Query("""
			select distinct t from Task t
			join fetch t.column
			left join fetch t.assignees
			left join fetch t.labels
			where t.space.id = :spaceId
			and t.archived = false
			order by t.position asc
			""")
	List<Task> findBySpaceId(@Param("spaceId") UUID spaceId);

	@Query("""
			select t from Task t
			where t.space.id = :spaceId
			and exists (select assignee from t.assignees assignee where assignee.id = :userId)
			""")
	List<Task> findAssignedTo(@Param("spaceId") UUID spaceId, @Param("userId") UUID userId);

	@Query("""
			select distinct t from Task t
			join fetch t.space
			join fetch t.column
			where exists (select assignee from t.assignees assignee where assignee.id = :userId)
			and t.archived = false
			and exists (
				select membership from SpaceMember membership
				where membership.space = t.space and membership.user.id = :userId
			)
			""")
	List<Task> findAssignedToUser(@Param("userId") UUID userId);

	@Query("""
			select distinct t from Task t
			join fetch t.column
			left join fetch t.assignees
			left join fetch t.labels
			where t.space.id = :spaceId
			and t.archived = true
			order by t.updatedAt desc
			""")
	List<Task> findArchivedBySpaceId(@Param("spaceId") UUID spaceId);

	List<Task> findByColumnIdOrderByPositionAsc(UUID columnId);

	Optional<Task> findByIdAndSpaceId(UUID id, UUID spaceId);

	@Modifying
	@Query("delete from Task t where t.column.id = :columnId")
	void deleteByColumnId(@Param("columnId") UUID columnId);

	@Modifying
	@Query("delete from Task t where t.space.id = :spaceId")
	void deleteBySpaceId(@Param("spaceId") UUID spaceId);

}
