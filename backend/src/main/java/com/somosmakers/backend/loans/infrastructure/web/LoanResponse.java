package com.somosmakers.backend.loans.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;

import com.somosmakers.backend.loans.domain.Loan;
import com.somosmakers.backend.loans.domain.LoanStatus;

public record LoanResponse(
		Long id,
		String userName,
		String userEmail,
		BigDecimal amount,
		int termMonths,
		LoanStatus status,
		Instant requestedAt,
		Instant decidedAt,
		String decidedBy) {

	public static LoanResponse from(Loan loan) {
		return new LoanResponse(loan.id(), loan.userName(), loan.userEmail(), loan.amount(), loan.termMonths(),
				loan.status(), loan.requestedAt(), loan.decidedAt(), loan.decidedBy());
	}
}
