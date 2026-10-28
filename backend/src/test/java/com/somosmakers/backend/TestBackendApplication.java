package com.somosmakers.backend;

import org.springframework.boot.builder.SpringApplicationBuilder;

public class TestBackendApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(BackendApplication.class)
			.profiles("test")
			.sources(TestcontainersConfiguration.class)
			.run(args);
	}

}
