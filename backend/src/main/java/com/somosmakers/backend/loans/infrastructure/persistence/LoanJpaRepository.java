package com.somosmakers.backend.loans.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanJpaRepository extends JpaRepository<LoanEntity, Long> {

	List<LoanEntity> findByUserEmailOrderByRequestedAtDesc(String userEmail);

	List<LoanEntity> findAllByOrderByRequestedAtDesc();
}
