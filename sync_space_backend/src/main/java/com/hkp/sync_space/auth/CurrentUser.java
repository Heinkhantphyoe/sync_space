package com.hkp.sync_space.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;

import com.hkp.sync_space.common.ApiException;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static AuthUser get() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Login required");
		}
		return user;
	}

}
