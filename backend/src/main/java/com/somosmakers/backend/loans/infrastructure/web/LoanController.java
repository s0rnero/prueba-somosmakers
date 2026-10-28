package com.somosmakers.backend.loans.infrastructure.web;

import java.util.List;
import java.util.Optional;

import com.somosmakers.backend.loans.application.LoanService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

	private static final List<String> NAME_CLAIMS = List.of("name", "preferred_username", "given_name");

	private final LoanService service;

	public LoanController(LoanService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse create(@Valid @RequestBody CreateLoanRequest request, Authentication authentication) {
		Actor actor = Actor.from(authentication);
		return LoanResponse.from(service.request(actor.email(), actor.name(), request.amount(), request.termMonths()));
	}

	@GetMapping("/mine")
	public List<LoanResponse> mine(Authentication authentication) {
		return service.listMine(Actor.from(authentication).email()).stream().map(LoanResponse::from).toList();
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<LoanResponse> all() {
		return service.listAll().stream().map(LoanResponse::from).toList();
	}

	@PatchMapping("/{id}/decision")
	@PreAuthorize("hasRole('ADMIN')")
	public LoanResponse decide(@PathVariable long id, @Valid @RequestBody DecisionRequest request,
			Authentication authentication) {
		return LoanResponse.from(service.decide(id, request.decision(), Actor.from(authentication).email()));
	}

	private record Actor(String email, String name) {

		static Actor from(Authentication authentication) {
			String email = emailFrom(authentication).orElseGet(authentication::getName);
			return new Actor(email, nameFrom(authentication).orElseGet(() -> defaultName(email)));
		}

		private static Optional<String> emailFrom(Authentication authentication) {
			if (authentication instanceof JwtAuthenticationToken jwt) {
				Object value = jwt.getTokenAttributes().get("email");
				if (value instanceof String text && !text.isBlank()) {
					return Optional.of(text);
				}
			}
			return Optional.empty();
		}

		private static Optional<String> nameFrom(Authentication authentication) {
			if (authentication instanceof JwtAuthenticationToken jwt) {
				for (String claim : NAME_CLAIMS) {
					Object value = jwt.getTokenAttributes().get(claim);
					if (value instanceof String text && !text.isBlank()) {
						return Optional.of(text);
					}
				}
			}
			return Optional.empty();
		}

		private static String defaultName(String email) {
			int at = email.indexOf('@');
			return at > 0 ? email.substring(0, at) : email;
		}
	}
}
