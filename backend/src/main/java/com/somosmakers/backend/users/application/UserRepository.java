package com.somosmakers.backend.users.application;

import java.util.Optional;

import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {

	Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

	Optional<User> findByIdAndDeletedAtIsNull(Long id);

	Page<User> findAllByDeletedAtIsNull(Pageable pageable);

	boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

	long countByRoleAndDeletedAtIsNull(Role role);

	User save(User user);
}
