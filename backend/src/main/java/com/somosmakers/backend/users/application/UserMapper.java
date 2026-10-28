package com.somosmakers.backend.users.application;

import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.users.domain.User;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}
}
