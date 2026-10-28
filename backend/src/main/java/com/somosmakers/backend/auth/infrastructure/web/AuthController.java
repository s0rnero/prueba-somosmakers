package com.somosmakers.backend.auth.infrastructure.web;

import com.somosmakers.backend.dto.AuthResponse;
import com.somosmakers.backend.dto.LoginRequest;
import com.somosmakers.backend.dto.RegisterRequest;
import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.exception.ApiException;
import com.somosmakers.backend.auth.infrastructure.security.RefreshCookieService;
import com.somosmakers.backend.auth.application.AuthService;
import com.somosmakers.backend.auth.application.AuthSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshCookieService cookies;

	public AuthController(AuthService authService, RefreshCookieService cookies) {
		this.authService = authService;
		this.cookies = cookies;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return withSession(HttpStatus.CREATED, authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return withSession(HttpStatus.OK, authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = RefreshCookieService.COOKIE_NAME, required = false) String refreshToken,
			HttpServletResponse response) {
		try {
			AuthSession session = authService.refresh(refreshToken);
			response.addHeader(HttpHeaders.SET_COOKIE, cookies.create(session.refreshToken()).toString());
			return ResponseEntity.ok(authService.toResponse(session));
		}
		catch (ApiException exception) {
			response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
			throw exception;
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = RefreshCookieService.COOKIE_NAME, required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
			.build();
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			throw ApiException.unauthorized("Autenticación requerida");
		}
		try {
			return ResponseEntity.ok(authService.me(Long.valueOf(jwt.getSubject())));
		}
		catch (NumberFormatException exception) {
			throw ApiException.unauthorized("Token inválido");
		}
	}

	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
		csrfToken.getToken();
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<AuthResponse> withSession(HttpStatus status, AuthSession session) {
		return ResponseEntity.status(status)
			.header(HttpHeaders.SET_COOKIE, cookies.create(session.refreshToken()).toString())
			.body(authService.toResponse(session));
	}
}
