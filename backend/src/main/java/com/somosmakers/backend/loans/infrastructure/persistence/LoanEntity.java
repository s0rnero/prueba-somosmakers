package com.somosmakers.backend.loans.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import com.somosmakers.backend.loans.domain.Loan;
import com.somosmakers.backend.loans.domain.LoanStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "loans")
public class LoanEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "user_email", nullable = false, length = 180)
	private String userEmail;

	@Column(name = "user_name", nullable = false, length = 120)
	private String userName;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "term_months", nullable = false)
	private int termMonths;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LoanStatus status;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Column(name = "decided_at")
	private Instant decidedAt;

	@Column(name = "decided_by", length = 180)
	private String decidedBy;

	protected LoanEntity() {
	}

	static LoanEntity from(Loan loan) {
		LoanEntity entity = new LoanEntity();
		entity.id = loan.id();
		entity.version = loan.version();
		entity.userEmail = loan.userEmail();
		entity.userName = loan.userName();
		entity.amount = loan.amount();
		entity.termMonths = loan.termMonths();
		entity.status = loan.status();
		entity.requestedAt = loan.requestedAt();
		entity.decidedAt = loan.decidedAt();
		entity.decidedBy = loan.decidedBy();
		return entity;
	}

	Loan toDomain() {
		return new Loan(id, version, userEmail, userName, amount, termMonths, status, requestedAt, decidedAt, decidedBy);
	}
}
