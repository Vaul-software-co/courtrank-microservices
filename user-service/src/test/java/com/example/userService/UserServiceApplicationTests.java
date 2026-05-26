package com.example.userService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UserServiceApplicationTests {

	@Test
	void mainClass_shouldBeLoadable() {
		assertDoesNotThrow(() -> Class.forName(UserServiceApplication.class.getName()));
	}

}
