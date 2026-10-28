package com.somosmakers.backend.dto;

import com.somosmakers.backend.users.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		@NotBlank @Size(min = 2, max = 120) String name,
		@NotBlank @Email @Size(max = 320) String email,
		@NotNull Role role,
		@Size(min = 8, max = 72) String password) {
}
