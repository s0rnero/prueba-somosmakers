package com.somosmakers.backend.loans.infrastructure.web;

import com.somosmakers.backend.loans.domain.LoanDecision;

import jakarta.validation.constraints.NotNull;

public record DecisionRequest(@NotNull(message = "La decisión es obligatoria") LoanDecision decision) {
}
