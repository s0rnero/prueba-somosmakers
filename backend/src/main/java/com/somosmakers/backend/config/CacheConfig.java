package com.somosmakers.backend.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager("loans");
		manager.setCaffeine(Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofSeconds(30))
			.maximumSize(500)
			.recordStats());
		return manager;
	}
}
