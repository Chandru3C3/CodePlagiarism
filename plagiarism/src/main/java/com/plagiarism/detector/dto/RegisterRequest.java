package com.plagiarism.detector.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
	@NotBlank(message = "Full name is required")
	@Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
	private String fullName;

	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Please provide a valid email address")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password must contain uppercase, lowercase, number, and special character")
	private String password;

	public RegisterRequest(
			@NotBlank(message = "Full name is required") @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters") String fullName,
			@NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores") String username,
			@NotBlank(message = "Email is required") @Email(message = "Please provide a valid email address") String email,
			@NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password must contain uppercase, lowercase, number, and special character") String password) {
		super();
		this.fullName = fullName;
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public RegisterRequest() {
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}