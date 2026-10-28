package com.somosmakers.backend.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
	}

	private static SecretKey secretKey(JwtProperties properties) {
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(properties.secret());
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET must be a Base64-encoded value", exception);
		}
		if (decoded.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT_SECRET must decode to at least " + MIN_SECRET_BYTES + " bytes (256 bits)");
		}
		return new SecretKeySpec(decoded, "HmacSHA256");
	}
}
