package com.somosmakers.backend.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.dto.UserCreateRequest;
import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.dto.UserUpdateRequest;
import com.somosmakers.backend.exception.ApiException;
import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository users;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserService service;

	@BeforeEach
	void setUp() {
		service = new UserService(users, passwordEncoder, new UserMapper());
	}

	@Test
	void createsUserWithNormalizedEmailAndEncodedPassword() {
		when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("nuevo@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = service.create(
				new UserCreateRequest("  Cesar  ", " Nuevo@Test.com ", "password123", Role.ADMIN));

		assertThat(response.name()).isEqualTo("Cesar");
		assertThat(response.email()).isEqualTo("nuevo@test.com");
		assertThat(response.role()).isEqualTo(Role.ADMIN);
		verify(passwordEncoder).encode("password123");
	}

	@Test
	void rejectsDuplicateEmailOnCreate() {
		when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("nuevo@test.com")).thenReturn(true);

		assertThatThrownBy(() -> service.create(
				new UserCreateRequest("Cesar", "nuevo@test.com", "password123", Role.USER)))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getCode()).isEqualTo("EMAIL_IN_USE");
			});
		verify(users, never()).save(any(User.class));
	}

	@Test
	void updatesUserAndPasswordWhenProvided() {
		User existing = user(1L, "Viejo", "viejo@test.com", Role.USER);
		when(users.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
		when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("nuevo@test.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
		when(users.save(existing)).thenReturn(existing);

		UserResponse response = service.update(1L,
				new UserUpdateRequest("Nuevo", " Nuevo@Test.com ", Role.ADMIN, "new-password"), 9L);

		assertThat(response.name()).isEqualTo("Nuevo");
		assertThat(response.email()).isEqualTo("nuevo@test.com");
		assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
		assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
	}

	@Test
	void rejectsEmailOwnedByAnotherUser() {
		User existing = user(1L, "Viejo", "viejo@test.com", Role.USER);
		User other = user(2L, "Otro", "nuevo@test.com", Role.USER);
		when(users.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
		when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("nuevo@test.com")).thenReturn(Optional.of(other));

		assertThatThrownBy(() -> service.update(1L,
				new UserUpdateRequest("Nuevo", "nuevo@test.com", Role.USER, null), 9L))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getCode()).isEqualTo("EMAIL_IN_USE");
			});
	}

	@Test
	void rejectsRemovingTheLastAdmin() {
		User admin = user(1L, "Admin", "admin@test.com", Role.ADMIN);
		when(users.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
		when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@test.com")).thenReturn(Optional.of(admin));
		when(users.countByRoleAndDeletedAtIsNull(Role.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> service.update(1L,
				new UserUpdateRequest("Admin", "admin@test.com", Role.USER, null), 9L))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("LAST_ADMIN");
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
			});
	}

	@Test
	void preventsSelfDeletion() {
		User user = user(1L, "Cesar", "usuario@test.com", Role.USER);
		when(users.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.delete(1L, 1L))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("SELF_DELETE");
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
			});
	}

	@Test
	void rejectsDeletingTheLastAdmin() {
		User admin = user(1L, "Admin", "admin@test.com", Role.ADMIN);
		when(users.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
		when(users.countByRoleAndDeletedAtIsNull(Role.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> service.delete(1L, 2L))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getCode()).isEqualTo("LAST_ADMIN");
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
			});
	}

	@Test
	void softDeletesUser() {
		User user = user(2L, "Cesar", "usuario@test.com", Role.USER);
		when(users.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(user));
		when(users.save(user)).thenReturn(user);

		service.delete(2L, 1L);

		assertThat(user.getDeletedAt()).isNotNull();
		verify(users).save(user);
	}

	@Test
	void listsOnlyActiveUsers() {
		User user = user(1L, "Cesar", "usuario@test.com", Role.USER);
		Page<User> page = new PageImpl<>(List.of(user));
		when(users.findAllByDeletedAtIsNull(any())).thenReturn(page);

		Page<UserResponse> result = service.list(PageRequest.of(0, 20));

		assertThat(result.getContent()).singleElement().satisfies(response -> {
			assertThat(response.id()).isEqualTo(1L);
			assertThat(response.email()).isEqualTo("usuario@test.com");
		});
	}

	@Test
	void returnsNotFoundForUnknownUser() {
		when(users.findByIdAndDeletedAtIsNull(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(404L))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
			});
	}

	private static User user(long id, String name, String email, Role role) {
		User user = new User(name, email, "hash", role);
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
		return user;
	}
}
