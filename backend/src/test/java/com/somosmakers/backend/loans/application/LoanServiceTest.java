package com.somosmakers.backend.loans.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.loans.domain.Loan;
import com.somosmakers.backend.loans.domain.LoanAlreadyDecidedException;
import com.somosmakers.backend.loans.domain.LoanDecision;
import com.somosmakers.backend.loans.domain.LoanNotFoundException;
import com.somosmakers.backend.loans.domain.LoanStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

	@Mock
	private LoanRepository repository;

	private LoanService service;

	@BeforeEach
	void setUp() {
		service = new LoanService(repository);
	}

	@Test
	void requestCreatesPendingLoan() {
		when(repository.save(any(Loan.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));

		Loan loan = service.request("usuario@test.com", "Usuario", new BigDecimal("1000.00"), 12);

		assertThat(loan.id()).isEqualTo(10L);
		assertThat(loan.status()).isEqualTo(LoanStatus.PENDING);
		assertThat(loan.decidedAt()).isNull();
		assertThat(loan.requestedAt()).isNotNull();
	}

	@Test
	void approvePendingLoanSetsDecision() {
		when(repository.findById(7L)).thenReturn(Optional.of(pending(7L)));
		when(repository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Loan loan = service.decide(7L, LoanDecision.APPROVED, "admin@test.com");

		assertThat(loan.status()).isEqualTo(LoanStatus.APPROVED);
		assertThat(loan.decidedBy()).isEqualTo("admin@test.com");
		assertThat(loan.decidedAt()).isNotNull();
	}

	@Test
	void rejectPendingLoanSetsDecision() {
		when(repository.findById(8L)).thenReturn(Optional.of(pending(8L)));
		when(repository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Loan loan = service.decide(8L, LoanDecision.REJECTED, "admin@test.com");

		assertThat(loan.status()).isEqualTo(LoanStatus.REJECTED);
	}

	@Test
	void decidingTwiceFails() {
		Loan approved = pending(9L).decide(LoanDecision.APPROVED, "admin@test.com", Instant.now());
		when(repository.findById(9L)).thenReturn(Optional.of(approved));

		assertThatThrownBy(() -> service.decide(9L, LoanDecision.REJECTED, "admin@test.com"))
			.isInstanceOf(LoanAlreadyDecidedException.class);
	}

	@Test
	void missingLoanFails() {
		when(repository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.decide(404L, LoanDecision.APPROVED, "admin@test.com"))
			.isInstanceOf(LoanNotFoundException.class);
	}

	@Test
	void listMineDelegatesToRepository() {
		List<Loan> loans = List.of(pending(1L));
		when(repository.findByUserEmail("usuario@test.com")).thenReturn(loans);

		assertThat(service.listMine("usuario@test.com")).isSameAs(loans);
	}

	private static Loan pending(long id) {
		return new Loan(id, 0, "usuario@test.com", "Usuario", new BigDecimal("1000.00"), 12, LoanStatus.PENDING,
				Instant.parse("2026-10-01T10:00:00Z"), null, null);
	}

	private static Loan withId(Loan loan, long id) {
		return new Loan(id, loan.version(), loan.userEmail(), loan.userName(), loan.amount(), loan.termMonths(),
				loan.status(), loan.requestedAt(), loan.decidedAt(), loan.decidedBy());
	}
}
