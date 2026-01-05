package com.plagiarism.detector.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.plagiarism.detector.dto.ErrorResponse;
import com.plagiarism.detector.service.UserAlreadyExistsException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(),
				request.getDescription(false).replace("uri=", ""));
		return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
				ex.getMessage(), request.getDescription(false).replace("uri=", ""));
		return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex,
			WebRequest request) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		response.put("timestamp", java.time.LocalDateTime.now());
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("error", "Validation Failed");
		response.put("message", "Invalid input data");
		response.put("errors", errors);
		response.put("path", request.getDescription(false).replace("uri=", ""));

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"Internal Server Error", ex.getMessage(), request.getDescription(false).replace("uri=", ""));
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}