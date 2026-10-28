package com.somosmakers.backend.auth.application;

import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.auth.domain.RefreshToken;

public interface RefreshTokenRepository {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findAllByFamilyId(String familyId);

	RefreshToken save(RefreshToken token);
}
