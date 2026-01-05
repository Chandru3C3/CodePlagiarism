package com.plagiarism.detector.dto;

import com.plagiarism.detector.entity.User;

public class UserDTO {
	private Long id;
	private String username;
	private String email;
	private String fullName;

	public UserDTO() {
	}

	public UserDTO(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.email = user.getEmail();
		this.fullName = user.getFullName();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

}