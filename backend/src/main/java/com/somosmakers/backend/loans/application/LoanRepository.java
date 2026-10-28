package com.somosmakers.backend.loans.application;

import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.loans.domain.Loan;

public interface LoanRepository {

	Loan save(Loan loan);

	Optional<Loan> findById(long id);

	List<Loan> findByUserEmail(String userEmail);

	List<Loan> findAll();
}
