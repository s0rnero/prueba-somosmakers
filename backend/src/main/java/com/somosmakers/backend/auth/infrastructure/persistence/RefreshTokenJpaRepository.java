package com.somosmakers.backend.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.auth.domain.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findAllByFamilyId(String familyId);

	void deleteAllByExpiresAtBefore(Instant instant);
}
