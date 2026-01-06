package com.plagiarism.detector.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plagiarism.detector.dto.ApiResponse;
import com.plagiarism.detector.dto.ForgotPasswordRequest;
import com.plagiarism.detector.dto.ResetPasswordRequest;
import com.plagiarism.detector.entity.User;
import com.plagiarism.detector.repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class PasswordService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailService emailService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static final int TOKEN_LENGTH = 32;
	private static final int TOKEN_EXPIRY_HOURS = 1;

	@Transactional
	public ApiResponse forgotPassword(ForgotPasswordRequest request) {
		try {
			String email = request.getEmail().toLowerCase();
			Optional<User> userOptional = userRepository.findByEmail(email);

			// Always return success message for security (don't reveal if user exists)
			if (!userOptional.isPresent()) {
				return new ApiResponse(true,
						"If an account exists with this email, you will receive password reset instructions.");
			}

			User user = userOptional.get();

			// Generate secure random token
			String resetToken = generateSecureToken();

			// Hash the token before storing
			String hashedToken = passwordEncoder.encode(resetToken);

			// Set token and expiration
			user.setResetPasswordToken(hashedToken);
			user.setResetPasswordExpire(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
			userRepository.save(user);

			// Send email
			emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);

			return new ApiResponse(true, "Password reset email sent successfully");

		} catch (MessagingException e) {
			return new ApiResponse(false, "Error sending email. Please try again later.");
		} catch (Exception e) {
			return new ApiResponse(false, "An error occurred. Please try again.");
		}
	}

	@Transactional
	public ApiResponse resetPassword(ResetPasswordRequest request) {
		try {
			String token = request.getToken();
			String newPassword = request.getNewPassword();

			// Validate password strength
			if (!isPasswordStrong(newPassword)) {
				return new ApiResponse(false,
						"Password must contain at least 8 characters with uppercase, lowercase, and number");
			}

			// Find all users and check token manually (since we hashed it)
			// In production, consider using JWT tokens instead
			Optional<User> userOptional = findUserByResetToken(token);

			if (!userOptional.isPresent()) {
				return new ApiResponse(false, "Invalid or expired reset token");
			}

			User user = userOptional.get();

			// Check if token is expired
			if (user.getResetPasswordExpire().isBefore(LocalDateTime.now())) {
				return new ApiResponse(false, "Reset token has expired");
			}

			// Update password
			user.setPassword(passwordEncoder.encode(newPassword));
			user.setResetPasswordToken(null);
			user.setResetPasswordExpire(null);
			userRepository.save(user);

			// Send confirmation email
			emailService.sendPasswordResetConfirmationEmail(user.getEmail(), user.getUsername());

			return new ApiResponse(true, "Password reset successful. You can now login with your new password.");

		} catch (MessagingException e) {
			return new ApiResponse(false, "Password reset but failed to send confirmation email");
		} catch (Exception e) {
			return new ApiResponse(false, "Error resetting password. Please try again.");
		}
	}

	private Optional<User> findUserByResetToken(String token) {
		// Get all users with non-null reset tokens
		return userRepository.findAll().stream().filter(user -> user.getResetPasswordToken() != null)
				.filter(user -> passwordEncoder.matches(token, user.getResetPasswordToken())).findFirst();
	}

	private String generateSecureToken() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[TOKEN_LENGTH];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private boolean isPasswordStrong(String password) {
		if (password.length() < 8)
			return false;

		boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
		boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);

		return hasUpperCase && hasLowerCase && hasDigit;
	}
}