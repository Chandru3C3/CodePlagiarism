package com.plagiarism.detector.service;

public class UserAlreadyExistsException extends RuntimeException {
	public UserAlreadyExistsException(String message) {
		super(message);
	}
}