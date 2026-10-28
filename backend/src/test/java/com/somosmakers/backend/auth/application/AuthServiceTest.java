package com.somosmakers.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.somosmakers.backend.dto.AuthResponse;
import com.somosmakers.backend.dto.LoginRequest;
import com.somosmakers.backend.dto.RegisterRequest;
import com.somosmakers.backend.dto.UserResponse;
import com.somosmakers.backend.exception.ApiException;
import com.somosmakers.backend.users.application.UserMapper;
import com.somosmakers.backend.users.application.UserRepository;
import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository users;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenService refreshTokens;

	@Mock
	private JwtService jwtService;

	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(users, passwordEncoder, refreshTokens, jwtService, new UserMapper());
	}

	@Test
	void registersUserAndCreatesSession() {
		when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("nuevo@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hash");
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(refreshTokens.create(any(User.class))).thenReturn("refresh-token");
		when(jwtService.createAccessToken(any(User.class))).thenReturn("access-token");

		AuthSession session = service.register(new RegisterRequest(" Cesar ", " Nuevo@Test.com ", "password123"));

		assertThat(session.accessToken()).isEqualTo("access-token");
		assertThat(session.refreshToken()).isEqualTo("refresh-token");
		assertThat(session.user().name()).isEqualTo("Cesar");
		assertThat(session.user().email()).isEqualTo("nuevo@test.com");
		assertThat(session.user().role()).isEqualTo(Role.USER);
	}

	@Test
	void rejectsRegistrationWithExistingEmail() {
		when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("nuevo@test.com")).thenReturn(true);

		assertThatThrownBy(() -> service.register(new RegisterRequest("Cesar", "nuevo@test.com", "password123")))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getCode()).isEqualTo("EMAIL_IN_USE");
			});
		verify(users, never()).save(any(User.class));
	}

	@Test
	void logsInWithValidCredentials() {
		User user = user("Cesar", "usuario@test.com", "hash", Role.USER);
		when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("usuario@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
		when(refreshTokens.create(user)).thenReturn("refresh-token");
		when(jwtService.createAccessToken(user)).thenReturn("access-token");

		AuthSession session = service.login(new LoginRequest("Usuario@Test.com", "password123"));

		assertThat(session.accessToken()).isEqualTo("access-token");
		assertThat(session.refreshToken()).isEqualTo("refresh-token");
		assertThat(session.user().email()).isEqualTo("usuario@test.com");
	}

	@Test
	void rejectsUnknownEmailOnLogin() {
		when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("nadie@test.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("nadie@test.com", "password123")))
			.isInstanceOfSatisfying(ApiException.class, exception -> {
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
				assertThat(exception.getMessage()).isEqualTo("Correo o contraseña inválidos");
			});
	}

	@Test
	void rejectsWrongPassword() {
		User user = user("Cesar", "usuario@test.com", "hash", Role.USER);
		when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("usuario@test.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

		assertThatThrownBy(() -> service.login(new LoginRequest("usuario@test.com", "incorrecta")))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void rotatesRefreshToken() {
		User user = user("Cesar", "usuario@test.com", "hash", Role.USER);
		when(refreshTokens.rotate("old-refresh"))
			.thenReturn(new RefreshTokenService.Rotation("new-refresh", user));
		when(jwtService.createAccessToken(user)).thenReturn("access-token");

		AuthSession session = service.refresh("old-refresh");

		assertThat(session.accessToken()).isEqualTo("access-token");
		assertThat(session.refreshToken()).isEqualTo("new-refresh");
		assertThat(session.user().email()).isEqualTo("usuario@test.com");
	}

	@Test
	void delegatesLogoutToRefreshTokenService() {
		service.logout("refresh-token");

		verify(refreshTokens).revoke("refresh-token");
	}

	@Test
	void returnsCurrentUser() {
		User user = user("Cesar", "usuario@test.com", "hash", Role.USER);
		when(users.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(user));

		UserResponse response = service.me(7L);

		assertThat(response.email()).isEqualTo("usuario@test.com");
		assertThat(response.role()).isEqualTo(Role.USER);
	}

	@Test
	void rejectsMissingCurrentUser() {
		when(users.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.me(7L))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void buildsPublicAuthResponse() {
		User user = user("Cesar", "usuario@test.com", "hash", Role.USER);
		when(jwtService.expiresInSeconds()).thenReturn(900L);
		UserResponse userResponse = new UserMapper().toResponse(user);

		AuthResponse response = service.toResponse(new AuthSession("access-token", "refresh-token", userResponse));

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(900L);
		assertThat(response.user()).isSameAs(userResponse);
	}

	private static User user(String name, String email, String passwordHash, Role role) {
		return new User(name, email, passwordHash, role);
	}
}
