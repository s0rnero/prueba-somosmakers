package com.somosmakers.backend.auth.infrastructure.security;

import com.somosmakers.backend.config.JwtProperties;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshCookieService {

	public static final String COOKIE_NAME = "refresh_token";

	private final JwtProperties properties;

	public RefreshCookieService(JwtProperties properties) {
		this.properties = properties;
	}

	public ResponseCookie create(String value) {
		return ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite("Lax")
			.path("/api/auth")
			.maxAge(properties.refreshTtl())
			.build();
	}

	public ResponseCookie clear() {
		return ResponseCookie.from(COOKIE_NAME, "")
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite("Lax")
			.path("/api/auth")
			.maxAge(0)
			.build();
	}
}
