package com.plagiarism.detector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

	@NotBlank(message = "Token is required")
	private String token;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String newPassword;

	public ResetPasswordRequest(@NotBlank(message = "Token is required") String token,
			@NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String newPassword) {
		super();
		this.token = token;
		this.newPassword = newPassword;
	}

	public ResetPasswordRequest() {
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

}