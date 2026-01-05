package com.plagiarism.detector.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
	@NotBlank(message = "Email or username is required")
	private String emailOrUsername;

	@NotBlank(message = "Password is required")
	private String password;

	public LoginRequest() {
	}

	public LoginRequest(@NotBlank(message = "Email or username is required") String emailOrUsername,
			@NotBlank(message = "Password is required") String password) {
		super();
		this.emailOrUsername = emailOrUsername;
		this.password = password;
	}

	public String getEmailOrUsername() {
		return emailOrUsername;
	}

	public void setEmailOrUsername(String emailOrUsername) {
		this.emailOrUsername = emailOrUsername;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}