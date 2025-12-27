package com.plagiarism.detector.dto;

import java.util.List;

public class UploadResponse {
	private String message;
	private List<Long> ids;

	// Default Constructor
	public UploadResponse() {
	}

	// Parameterized Constructor
	public UploadResponse(String message, List<Long> ids) {
		this.message = message;
		this.ids = ids;
	}

	// Getters and Setters
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}
}