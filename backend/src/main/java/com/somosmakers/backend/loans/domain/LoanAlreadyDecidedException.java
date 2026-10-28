package com.somosmakers.backend.loans.domain;

public class LoanAlreadyDecidedException extends RuntimeException {

	public LoanAlreadyDecidedException(Long id) {
		super("El préstamo " + id + " ya fue decidido");
	}
}
