package com.somosmakers.backend.loans.domain;

public enum LoanDecision {

	APPROVED(LoanStatus.APPROVED),
	REJECTED(LoanStatus.REJECTED);

	private final LoanStatus status;

	LoanDecision(LoanStatus status) {
		this.status = status;
	}

	public LoanStatus status() {
		return status;
	}
}
