package com.somosmakers.backend.users.infrastructure.persistence;

import java.util.Optional;

import com.somosmakers.backend.users.application.UserRepository;
import com.somosmakers.backend.users.domain.Role;
import com.somosmakers.backend.users.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements UserRepository {

	private final UserJpaRepository repository;

	public UserPersistenceAdapter(UserJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email) {
		return repository.findByEmailIgnoreCaseAndDeletedAtIsNull(email);
	}

	@Override
	public Optional<User> findByIdAndDeletedAtIsNull(Long id) {
		return repository.findByIdAndDeletedAtIsNull(id);
	}

	@Override
	public Page<User> findAllByDeletedAtIsNull(Pageable pageable) {
		return repository.findAllByDeletedAtIsNull(pageable);
	}

	@Override
	public boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email) {
		return repository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
	}

	@Override
	public long countByRoleAndDeletedAtIsNull(Role role) {
		return repository.countByRoleAndDeletedAtIsNull(role);
	}

	@Override
	public User save(User user) {
		return repository.save(user);
	}
}
