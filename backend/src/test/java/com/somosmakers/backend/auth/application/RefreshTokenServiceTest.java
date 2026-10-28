package com.somosmakers.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.auth.domain.RefreshToken;
import com.somosmakers.backend.config.JwtProperties;
import com.somosmakers.backend.exception.ApiException;
import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository repository;

	private RefreshTokenService service;

	private User user;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties("secret", "somosmakers-prestamos",
				Duration.ofMinutes(15), Duration.ofDays(7), true);
		service = new RefreshTokenService(repository, properties);
		user = new User("Cesar", "usuario@test.com", "hash", Role.USER);
	}

	@Test
	void createsHashedTokenThatIsNotStoredInPlainText() {
		stubSave();
		String raw = service.create(user);

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		RefreshToken stored = captor.getValue();

		assertThat(raw).isNotBlank();
		assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(raw);
		assertThat(stored.getExpiresAt()).isAfter(Instant.now());
		assertThat(stored.getUser()).isSameAs(user);
	}

	@Test
	void rotatesAValidRefreshTokenAndRevokesThePreviousOne() {
		stubSave();
		String raw = service.create(user);
		RefreshToken current = savedToken();
		clearInvocations(repository);
		when(repository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

		RefreshTokenService.Rotation rotation = service.rotate(raw);

		assertThat(rotation.refreshToken()).isNotBlank().isNotEqualTo(raw);
		assertThat(rotation.user()).isSameAs(user);
		assertThat(current.getRevokedAt()).isNotNull();
		verify(repository).findByTokenHash(current.getTokenHash());
		verify(repository, org.mockito.Mockito.atLeastOnce()).save(any(RefreshToken.class));
	}

	@Test
	void rejectsAnAlreadyRevokedTokenAndRevokesItsFamily() {
		stubSave();
		String raw = service.create(user);
		RefreshToken current = savedToken();
		current.setRevokedAt(Instant.now());
		when(repository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));
		when(repository.findAllByFamilyId(current.getFamilyId())).thenReturn(List.of(current));

		assertThatThrownBy(() -> service.rotate(raw))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(repository).findAllByFamilyId(current.getFamilyId());
	}

	@Test
	void rejectsAnExpiredToken() {
		stubSave();
		String raw = service.create(user);
		RefreshToken current = savedToken();
		ReflectionTestUtils.setField(current, "expiresAt", Instant.now().minusSeconds(1));
		when(repository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));
		when(repository.findAllByFamilyId(current.getFamilyId())).thenReturn(List.of(current));

		assertThatThrownBy(() -> service.rotate(raw))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void rejectsMissingOrBlankToken() {
		assertThatThrownBy(() -> service.rotate(null))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(repository, never()).findByTokenHash(any());
	}

	@Test
	void revokeMarksTokenAsRevoked() {
		stubSave();
		String raw = service.create(user);
		RefreshToken current = savedToken();
		clearInvocations(repository);
		when(repository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

		service.revoke(raw);

		assertThat(current.getRevokedAt()).isNotNull();
		verify(repository).save(current);
	}

	@Test
	void revokeIsIdempotentAndIgnoresBlankTokens() {
		service.revoke(" ");
		verify(repository, never()).findByTokenHash(any());

		stubSave();
		String raw = service.create(user);
		RefreshToken current = savedToken();
		clearInvocations(repository);
		current.setRevokedAt(Instant.now());
		when(repository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

		service.revoke(raw);

		verify(repository, never()).save(current);
	}

	private void stubSave() {
		when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private RefreshToken savedToken() {
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}
}
