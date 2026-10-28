package com.somosmakers.backend.users.infrastructure.web;

import java.net.URI;

import com.somosmakers.backend.dto.UserCreateRequest;
import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.dto.UserUpdateRequest;
import com.somosmakers.backend.users.application.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public Page<UserResponse> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return userService.list(pageable);
	}

	@GetMapping("/{id}")
	public UserResponse get(@PathVariable Long id) {
		return userService.get(id);
	}

	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
		UserResponse created = userService.create(request);
		return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return userService.update(id, request, currentUserId(jwt));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		userService.delete(id, currentUserId(jwt));
		return ResponseEntity.noContent().build();
	}

	private static Long currentUserId(Jwt jwt) {
		return jwt == null ? null : Long.valueOf(jwt.getSubject());
	}
}
