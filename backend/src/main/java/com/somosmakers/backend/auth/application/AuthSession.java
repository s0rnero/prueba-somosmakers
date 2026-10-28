package com.somosmakers.backend.auth.application;

import com.somosmakers.backend.dto.UserResponse;

public record AuthSession(
		String accessToken,
		String refreshToken,
		UserResponse user) {
}
