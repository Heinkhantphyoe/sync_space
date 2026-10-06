package com.hkp.sync_space.config;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.hkp.sync_space.space.SpaceMemberRepository;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private final SpaceMemberRepository memberRepository;

	public StompAuthChannelInterceptor(SpaceMemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (accessor.getCommand() == null || accessor.getSessionAttributes() == null) {
			return message;
		}
		if (StompCommand.CONNECT.equals(accessor.getCommand()) && accessor.getSessionAttributes().get("userId") == null) {
			throw new AccessDeniedException("Login required");
		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			UUID userId = (UUID) accessor.getSessionAttributes().get("userId");
			UUID connectedSpace = (UUID) accessor.getSessionAttributes().get("spaceId");
			UUID destinationSpace = spaceId(accessor.getDestination());
			if (userId == null || connectedSpace == null || destinationSpace == null
					|| !connectedSpace.equals(destinationSpace)
					|| !memberRepository.existsBySpaceIdAndUserId(destinationSpace, userId)) {
				throw new AccessDeniedException("You are not a member of this space");
			}
		}
		return message;
	}

	private static UUID spaceId(String destination) {
		if (destination == null || !destination.startsWith("/topic/spaces/")) {
			return null;
		}
		try {
			return UUID.fromString(destination.substring("/topic/spaces/".length()));
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

}
