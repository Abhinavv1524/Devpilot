package com.devpilot.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.devpilot.auth.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
	}

	@Test
	void signupStoresHashedPasswordAndReturnsUser() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.email").value("alice@example.com"));

		var user = userRepository.findByUsername("alice").orElseThrow();
		org.junit.jupiter.api.Assertions.assertNotEquals("password123", user.getPassword());
		org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("password123", user.getPassword()));
	}

	@Test
	void signupRejectsDuplicateUsernameAndInvalidFields() throws Exception {
		createUser();

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"email\":\"other@example.com\",\"password\":\"password123\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"a\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"));
	}

	@Test
	void loginReturnsBearerJwtForValidCredentials() throws Exception {
		createUser();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresAt").isNotEmpty());
	}

	@Test
	void loginRejectsInvalidCredentials() throws Exception {
		createUser();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value(containsString("Invalid username")));
	}

	private void createUser() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
				.andExpect(status().isCreated());
	}

}
