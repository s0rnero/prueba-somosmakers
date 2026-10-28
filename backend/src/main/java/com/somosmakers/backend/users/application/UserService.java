package com.somosmakers.backend.users.application;

import java.time.Instant;
import java.util.Locale;

import com.somosmakers.backend.dto.UserCreateRequest;
import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.dto.UserUpdateRequest;
import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;
import com.somosmakers.backend.exception.ApiException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper mapper;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder, UserMapper mapper) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.mapper = mapper;
	}

	@Transactional
	public UserResponse create(UserCreateRequest request) {
		String email = normalizeEmail(request.email());
		if (users.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
			throw ApiException.conflict("EMAIL_IN_USE", "El correo ya está registrado");
		}
		User user = users.save(new User(
			request.name().trim(),
			email,
			passwordEncoder.encode(request.password()),
			request.role()));
		return mapper.toResponse(user);
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> list(Pageable pageable) {
		return users.findAllByDeletedAtIsNull(pageable).map(mapper::toResponse);
	}

	@Transactional(readOnly = true)
	public UserResponse get(Long id) {
		return mapper.toResponse(find(id));
	}

	@Transactional
	public UserResponse update(Long id, UserUpdateRequest request, Long currentUserId) {
		User user = find(id);
		String email = normalizeEmail(request.email());
		users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(existing -> {
			if (!existing.getId().equals(id)) {
				throw ApiException.conflict("EMAIL_IN_USE", "El correo ya está registrado");
			}
		});
		if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN
				&& users.countByRoleAndDeletedAtIsNull(Role.ADMIN) <= 1) {
			throw ApiException.conflict("LAST_ADMIN", "No se puede dejar el sistema sin administradores");
		}
		user.setName(request.name().trim());
		user.setEmail(email);
		user.setRole(request.role());
		if (request.password() != null && !request.password().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(request.password()));
		}
		return mapper.toResponse(users.save(user));
	}

	@Transactional
	public void delete(Long id, Long currentUserId) {
		User user = find(id);
		if (id.equals(currentUserId)) {
			throw ApiException.conflict("SELF_DELETE", "No puedes eliminar tu propio usuario");
		}
		if (user.getRole() == Role.ADMIN && users.countByRoleAndDeletedAtIsNull(Role.ADMIN) <= 1) {
			throw ApiException.conflict("LAST_ADMIN", "No se puede dejar el sistema sin administradores");
		}
		user.setDeletedAt(Instant.now());
		users.save(user);
	}

	private User find(Long id) {
		return users.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
