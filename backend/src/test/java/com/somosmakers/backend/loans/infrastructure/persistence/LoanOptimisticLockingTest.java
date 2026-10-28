package com.somosmakers.backend.loans.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import com.somosmakers.backend.TestcontainersConfiguration;
import com.somosmakers.backend.loans.application.LoanRepository;
import com.somosmakers.backend.loans.domain.Loan;
import com.somosmakers.backend.loans.domain.LoanDecision;
import com.somosmakers.backend.loans.domain.LoanStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LoanOptimisticLockingTest {

	@Autowired
	private LoanRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void staleDecisionIsRejectedWithOptimisticLock() {
		Loan created = repository.save(Loan.requested(
				"concurrencia@test.com", "Concurrencia", new BigDecimal("1000.00"), 12, Instant.now()));

		try {
			Loan first = repository.findById(created.id()).orElseThrow();
			Loan stale = repository.findById(created.id()).orElseThrow();

			repository.save(first.decide(LoanDecision.APPROVED, "admin@test.com", Instant.now()));

			assertThatThrownBy(() -> repository.save(stale.decide(LoanDecision.REJECTED, "admin@test.com", Instant.now())))
				.isInstanceOf(OptimisticLockingFailureException.class);

			assertThat(repository.findById(created.id()).orElseThrow().status()).isEqualTo(LoanStatus.APPROVED);
		}
		finally {
			jdbcTemplate.update("DELETE FROM loans WHERE id = ?", created.id());
		}
	}
}
