package com.somosmakers.backend.exception;

import java.util.UUID;

import com.somosmakers.backend.dto.ApiError;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
		return ResponseEntity.status(exception.getStatus())
			.body(new ApiError(exception.getCode(), exception.getMessage(), requestId(request)));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.orElse("Datos inválidos");
		return ResponseEntity.badRequest()
			.body(new ApiError("VALIDATION_ERROR", message, requestId(request)));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ApiError("CONFLICT", "El recurso entra en conflicto con uno existente", requestId(request)));
	}

	private static String requestId(HttpServletRequest request) {
		Object requestId = request.getAttribute("requestId");
		return requestId != null ? requestId.toString() : UUID.randomUUID().toString();
	}
}
