package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hkp.sync_space.auth.AuthUser;
import com.hkp.sync_space.common.ApiException;

@Service
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	public List<NotificationResponse> recent(AuthUser actor) {
		return notificationRepository.findRecent(actor.id(), PageRequest.of(0, 30)).stream()
				.map(NotificationService::toResponse)
				.toList();
	}

	@Transactional
	public NotificationResponse markRead(UUID notificationId, AuthUser actor) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, actor.id())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
		if (notification.getReadAt() == null) {
			notification.setReadAt(Instant.now());
		}
		return toResponse(notification);
	}

	@Transactional
	public void markAllRead(AuthUser actor) {
		Instant now = Instant.now();
		for (Notification notification : notificationRepository.findUnread(actor.id())) {
			notification.setReadAt(now);
		}
	}

	static NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getKind().name(), notification.getUser().getId(),
				notification.getSpace().getId(), notification.getSpace().getName(), notification.getTask().getId(),
				notification.getTask().getTitle(), notification.getActor().getId(), notification.getActor().getDisplayName(),
				notification.getBody(), notification.getCreatedAt(), notification.getReadAt() != null);
	}

}
