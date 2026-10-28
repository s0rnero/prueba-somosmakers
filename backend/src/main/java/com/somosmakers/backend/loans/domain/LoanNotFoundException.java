package com.somosmakers.backend.loans.domain;

public class LoanNotFoundException extends RuntimeException {

	public LoanNotFoundException(Long id) {
		super("No existe el préstamo " + id);
	}
}
