package com.somosmakers.backend.auth.application;

import java.util.Locale;

import com.somosmakers.backend.dto.AuthResponse;
import com.somosmakers.backend.dto.LoginRequest;
import com.somosmakers.backend.dto.RegisterRequest;
import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;
import com.somosmakers.backend.exception.ApiException;
import com.somosmakers.backend.users.application.UserMapper;
import com.somosmakers.backend.users.application.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokens;
	private final JwtService jwtService;
	private final UserMapper mapper;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokens, JwtService jwtService, UserMapper mapper) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokens = refreshTokens;
		this.jwtService = jwtService;
		this.mapper = mapper;
	}

	@Transactional
	public AuthSession register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (users.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
			throw ApiException.conflict("EMAIL_IN_USE", "El correo ya está registrado");
		}
		User user = users.save(new User(
			request.name().trim(),
			email,
			passwordEncoder.encode(request.password()),
			Role.USER));
		return session(user);
	}

	@Transactional
	public AuthSession login(LoginRequest request) {
		User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(request.email()))
			.orElseThrow(() -> ApiException.unauthorized("Correo o contraseña inválidos"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized("Correo o contraseña inválidos");
		}
		return session(user);
	}

	@Transactional
	public AuthSession refresh(String rawToken) {
		RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawToken);
		User user = rotation.user();
		return new AuthSession(
			jwtService.createAccessToken(user),
			rotation.refreshToken(),
			mapper.toResponse(user));
	}

	@Transactional
	public void logout(String rawToken) {
		refreshTokens.revoke(rawToken);
	}

	@Transactional(readOnly = true)
	public UserResponse me(Long userId) {
		User user = users.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> ApiException.unauthorized("Usuario no disponible"));
		return mapper.toResponse(user);
	}

	public AuthResponse toResponse(AuthSession session) {
		return new AuthResponse(
			session.accessToken(),
			"Bearer",
			jwtService.expiresInSeconds(),
			session.user());
	}

	private AuthSession session(User user) {
		return new AuthSession(
			jwtService.createAccessToken(user),
			refreshTokens.create(user),
			mapper.toResponse(user));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
