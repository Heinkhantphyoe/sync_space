package com.hkp.sync_space.config;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.hkp.sync_space.space.SpaceMemberRepository;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	private final JwtService jwtService;
	private final SpaceMemberRepository memberRepository;

	public JwtHandshakeInterceptor(JwtService jwtService, SpaceMemberRepository memberRepository) {
		this.jwtService = jwtService;
		this.memberRepository = memberRepository;
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
		String token = params.getFirst("token");
		String spaceIdValue = params.getFirst("spaceId");
		if (token == null || token.isBlank() || spaceIdValue == null || spaceIdValue.isBlank()) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		try {
			UUID userId = jwtService.parseUserId(token);
			UUID spaceId = UUID.fromString(spaceIdValue);
			if (!memberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
				response.setStatusCode(HttpStatus.FORBIDDEN);
				return false;
			}
			attributes.put("userId", userId);
			attributes.put("spaceId", spaceId);
			return true;
		}
		catch (RuntimeException exception) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}

}
