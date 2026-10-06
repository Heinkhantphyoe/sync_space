package com.hkp.sync_space.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hkp.sync_space.common.ApiException;
import com.hkp.sync_space.config.JwtService;
import com.hkp.sync_space.user.User;
import com.hkp.sync_space.user.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists");
		}
		User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim());
		userRepository.save(user);
		return tokenFor(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email().trim().toLowerCase())
				.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));
		return tokenFor(user);
	}

	@Transactional(readOnly = true)
	public UserResponse me(AuthUser current) {
		User user = userRepository.findById(current.id())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Login required"));
		return toResponse(user);
	}

	private AuthResponse tokenFor(User user) {
		return new AuthResponse(jwtService.createToken(user.getId()), toResponse(user));
	}

	private static UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
	}

}
