package com.somosmakers.backend.loans.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.loans.application.LoanRepository;
import com.somosmakers.backend.loans.domain.Loan;

import org.springframework.stereotype.Component;

@Component
public class LoanPersistenceAdapter implements LoanRepository {

	private final LoanJpaRepository repository;

	public LoanPersistenceAdapter(LoanJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Loan save(Loan loan) {
		return repository.save(LoanEntity.from(loan)).toDomain();
	}

	@Override
	public Optional<Loan> findById(long id) {
		return repository.findById(id).map(LoanEntity::toDomain);
	}

	@Override
	public List<Loan> findByUserEmail(String userEmail) {
		return repository.findByUserEmailOrderByRequestedAtDesc(userEmail).stream().map(LoanEntity::toDomain).toList();
	}

	@Override
	public List<Loan> findAll() {
		return repository.findAllByOrderByRequestedAtDesc().stream().map(LoanEntity::toDomain).toList();
	}
}
