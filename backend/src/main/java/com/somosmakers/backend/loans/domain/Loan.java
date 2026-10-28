package com.somosmakers.backend.loans.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Loan(
		Long id,
		long version,
		String userEmail,
		String userName,
		BigDecimal amount,
		int termMonths,
		LoanStatus status,
		Instant requestedAt,
		Instant decidedAt,
		String decidedBy) {

	public static Loan requested(String userEmail, String userName, BigDecimal amount, int termMonths,
			Instant requestedAt) {
		return new Loan(null, 0, userEmail, userName, amount, termMonths, LoanStatus.PENDING, requestedAt, null, null);
	}

	public Loan decide(LoanDecision decision, String adminEmail, Instant decidedAt) {
		if (status != LoanStatus.PENDING) {
			throw new LoanAlreadyDecidedException(id);
		}
		return new Loan(id, version, userEmail, userName, amount, termMonths, decision.status(), requestedAt, decidedAt,
				adminEmail);
	}
}
