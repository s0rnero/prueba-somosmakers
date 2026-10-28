package com.somosmakers.backend.dto;

import java.time.Instant;

import com.somosmakers.backend.users.domain.Role;

public record UserResponse(
		Long id,
		String name,
		String email,
		Role role,
		Instant createdAt,
		Instant updatedAt) {
}
