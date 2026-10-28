package com.somosmakers.backend.loans.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.somosmakers.backend.loans.domain.Loan;
import com.somosmakers.backend.loans.domain.LoanDecision;
import com.somosmakers.backend.loans.domain.LoanNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

	private final LoanRepository repository;

	public LoanService(LoanRepository repository) {
		this.repository = repository;
	}

	@Transactional
	@CacheEvict(cacheNames = "loans", allEntries = true)
	public Loan request(String userEmail, String userName, BigDecimal amount, int termMonths) {
		return repository.save(Loan.requested(userEmail, userName, amount, termMonths, Instant.now()));
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "loans", key = "'mine:' + #email")
	public List<Loan> listMine(String email) {
		return repository.findByUserEmail(email);
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "loans", key = "'all'")
	public List<Loan> listAll() {
		return repository.findAll();
	}

	@Transactional
	@CacheEvict(cacheNames = "loans", allEntries = true)
	public Loan decide(long id, LoanDecision decision, String adminEmail) {
		Loan loan = repository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));
		return repository.save(loan.decide(decision, adminEmail, Instant.now()));
	}
}
