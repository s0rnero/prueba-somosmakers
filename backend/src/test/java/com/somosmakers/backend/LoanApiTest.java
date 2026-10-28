package com.somosmakers.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.somosmakers.backend.loans.application.LoanRepository;
import com.somosmakers.backend.loans.domain.Loan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoanApiTest {

	private static final String USER_EMAIL = "usuario@test.com";

	private static final String ADMIN_EMAIL = "admin@test.com";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private LoanRepository repository;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void clearCache() {
		cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
	}

	@Test
	void userCanRequestLoan() throws Exception {
		mockMvc
			.perform(post("/api/loans").header("Authorization", bearer(USER_EMAIL, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 1500.00, "termMonths": 6}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.userEmail").value(USER_EMAIL))
			.andExpect(jsonPath("$.amount").value(1500.00));
	}

	@Test
	void userCannotListEveryLoan() throws Exception {
		mockMvc.perform(get("/api/loans").header("Authorization", bearer(USER_EMAIL, "USER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanApproveOnlyOnce() throws Exception {
		Loan loan = repository.save(
				Loan.requested("aprobar@test.com", "Aprobar", new BigDecimal("1200.00"), 10, Instant.now()));

		mockMvc
			.perform(patch("/api/loans/" + loan.id() + "/decision").header("Authorization", bearer(ADMIN_EMAIL, "ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"decision": "APPROVED"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.decidedBy").value(ADMIN_EMAIL));

		mockMvc
			.perform(patch("/api/loans/" + loan.id() + "/decision").header("Authorization", bearer(ADMIN_EMAIL, "ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"decision": "REJECTED"}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("LOAN_ALREADY_DECIDED"));
	}

	@Test
	void amountOutOfRangeIsRejected() throws Exception {
		mockMvc
			.perform(post("/api/loans").header("Authorization", bearer(USER_EMAIL, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 50.00, "termMonths": 6}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("LOAN_VALIDATION_ERROR"));
	}

	@Test
	void anonymousIsRejected() throws Exception {
		mockMvc.perform(get("/api/loans/mine")).andExpect(status().isUnauthorized());
	}

	private String bearer(String email, String role) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("somosmakers-prestamos")
			.subject("42")
			.claim("email", email)
			.claim("roles", List.of(role))
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(600))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
