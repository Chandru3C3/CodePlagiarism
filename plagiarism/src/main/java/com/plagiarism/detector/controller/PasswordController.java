package com.plagiarism.detector.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plagiarism.detector.dto.ApiResponse;
import com.plagiarism.detector.dto.ForgotPasswordRequest;
import com.plagiarism.detector.dto.ResetPasswordRequest;
import com.plagiarism.detector.service.PasswordService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cpd/auth")
public class PasswordController {

	@Autowired
	private PasswordService passwordService;

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
			BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
			return ResponseEntity.badRequest().body(new ApiResponse(false, errorMessage));
		}

		ApiResponse response = passwordService.forgotPassword(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
			BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
			return ResponseEntity.badRequest().body(new ApiResponse(false, errorMessage));
		}

		ApiResponse response = passwordService.resetPassword(request);

		if (!response.isSuccess()) {
			return ResponseEntity.badRequest().body(response);
		}

		return ResponseEntity.ok(response);
	}
}