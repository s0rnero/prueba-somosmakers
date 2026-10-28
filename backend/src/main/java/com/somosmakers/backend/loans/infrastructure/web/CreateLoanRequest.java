package com.somosmakers.backend.loans.infrastructure.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateLoanRequest(
		@NotNull(message = "El monto es obligatorio")
		@DecimalMin(value = "100.00", message = "El monto mínimo es 100")
		@DecimalMax(value = "50000.00", message = "El monto máximo es 50000")
		BigDecimal amount,

		@NotNull(message = "El plazo es obligatorio")
		@Min(value = 1, message = "El plazo mínimo es 1 mes")
		@Max(value = 60, message = "El plazo máximo es 60 meses")
		Integer termMonths) {
}
