package com.somosmakers.backend.loans.infrastructure.web;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp) {
}
