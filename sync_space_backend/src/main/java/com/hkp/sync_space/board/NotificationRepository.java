package com.hkp.sync_space.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	@Query("""
			select notification from Notification notification
			join fetch notification.user
			join fetch notification.space
			join fetch notification.task
			join fetch notification.actor
			where notification.user.id = :userId
			order by notification.createdAt desc
			""")
	List<Notification> findRecent(@Param("userId") UUID userId, Pageable pageable);

	@Query("""
			select notification from Notification notification
			join fetch notification.user
			join fetch notification.space
			join fetch notification.task
			join fetch notification.actor
			where notification.id = :id and notification.user.id = :userId
			""")
	Optional<Notification> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

	@Query("""
			select notification from Notification notification
			where notification.user.id = :userId and notification.readAt is null
			""")
	List<Notification> findUnread(@Param("userId") UUID userId);

}
