package com.somosmakers.backend.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.auth.application.RefreshTokenRepository;
import com.somosmakers.backend.auth.domain.RefreshToken;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

	private final RefreshTokenJpaRepository repository;

	public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return repository.findByTokenHash(tokenHash);
	}

	@Override
	public List<RefreshToken> findAllByFamilyId(String familyId) {
		return repository.findAllByFamilyId(familyId);
	}

	@Override
	public RefreshToken save(RefreshToken token) {
		return repository.save(token);
	}
}
