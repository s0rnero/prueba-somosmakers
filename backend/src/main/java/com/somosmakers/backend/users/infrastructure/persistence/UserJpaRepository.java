package com.somosmakers.backend.users.infrastructure.persistence;

import java.util.Optional;

import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

	Optional<User> findByIdAndDeletedAtIsNull(Long id);

	Page<User> findAllByDeletedAtIsNull(Pageable pageable);

	boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

	long countByRoleAndDeletedAtIsNull(Role role);
}
