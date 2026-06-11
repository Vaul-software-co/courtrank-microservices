package com.example.socialService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SocialServiceApplicationTests {

	@Test
	void applicationCanBeInstantiated() {
		assertDoesNotThrow(SocialServiceApplication::new);
	}

}
