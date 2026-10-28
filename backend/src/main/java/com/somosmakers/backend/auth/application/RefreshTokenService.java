package com.somosmakers.backend.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.somosmakers.backend.config.JwtProperties;
import com.somosmakers.backend.auth.domain.RefreshToken;
import com.somosmakers.backend.users.domain.User;
import com.somosmakers.backend.exception.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository repository;
	private final JwtProperties properties;

	public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	public record Rotation(String refreshToken, User user) {
	}

	@Transactional
	public String create(User user) {
		return create(user, UUID.randomUUID().toString());
	}

	private String create(User user, String familyId) {
		String raw = randomToken();
		repository.save(new RefreshToken(
			user,
			hash(raw),
			familyId,
			Instant.now().plus(properties.refreshTtl())));
		return raw;
	}

	@Transactional
	public Rotation rotate(String rawToken) {
		RefreshToken current = find(rawToken);
		if (current.getRevokedAt() != null) {
			revokeFamily(current.getFamilyId());
			throw ApiException.unauthorized("Sesión inválida");
		}
		if (!current.getExpiresAt().isAfter(Instant.now())) {
			revokeFamily(current.getFamilyId());
			throw ApiException.unauthorized("Sesión expirada");
		}
		current.setRevokedAt(Instant.now());
		String nextRaw = randomToken();
		RefreshToken next = new RefreshToken(
			current.getUser(),
			hash(nextRaw),
			current.getFamilyId(),
			Instant.now().plus(properties.refreshTtl()));
		repository.save(next);
		current.setReplacedBy(next.getId());
		repository.save(current);
		return new Rotation(nextRaw, current.getUser());
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
			if (token.getRevokedAt() == null) {
				token.setRevokedAt(Instant.now());
				repository.save(token);
			}
		});
	}

	private RefreshToken find(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw ApiException.unauthorized("Sesión inválida");
		}
		return repository.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> ApiException.unauthorized("Sesión inválida"));
	}

	private void revokeFamily(String familyId) {
		repository.findAllByFamilyId(familyId).forEach(token -> {
			if (token.getRevokedAt() == null) {
				token.setRevokedAt(Instant.now());
				repository.save(token);
			}
		});
	}

	private static String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(value.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 no disponible", exception);
		}
	}
}
