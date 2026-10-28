package com.somosmakers.backend.loans.infrastructure.web;

import java.time.Instant;

import com.somosmakers.backend.loans.domain.LoanAlreadyDecidedException;
import com.somosmakers.backend.loans.domain.LoanNotFoundException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = LoanController.class)
public class LoanExceptionHandler {

	@ExceptionHandler(LoanNotFoundException.class)
	ResponseEntity<ApiError> handleNotFound(LoanNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "LOAN_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(LoanAlreadyDecidedException.class)
	ResponseEntity<ApiError> handleAlreadyDecided(LoanAlreadyDecidedException exception) {
		return error(HttpStatus.CONFLICT, "LOAN_ALREADY_DECIDED", exception.getMessage());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ApiError> handleConcurrentUpdate(OptimisticLockingFailureException exception) {
		return error(HttpStatus.CONFLICT, "LOAN_CONCURRENT_UPDATE",
				"La solicitud fue modificada por otra operación; vuelve a consultarla");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.findFirst()
			.map(fieldError -> fieldError.getDefaultMessage())
			.orElse("Solicitud inválida");
		return error(HttpStatus.BAD_REQUEST, "LOAN_VALIDATION_ERROR", message);
	}

	private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
	}
}
