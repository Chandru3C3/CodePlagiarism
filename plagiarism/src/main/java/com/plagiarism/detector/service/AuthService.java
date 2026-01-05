package com.plagiarism.detector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plagiarism.detector.config.JwtUtil;
import com.plagiarism.detector.dto.AuthResponse;
import com.plagiarism.detector.dto.LoginRequest;
import com.plagiarism.detector.dto.RegisterRequest;
import com.plagiarism.detector.dto.UserDTO;
import com.plagiarism.detector.entity.User;
import com.plagiarism.detector.repository.UserRepository;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		// Check if username already exists
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new UserAlreadyExistsException("Username is already taken");
		}

		// Check if email already exists
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new UserAlreadyExistsException("Email is already registered");
		}

		// Create new user
		User user = new User();
		user.setFullName(request.getFullName());
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setActive(true);

		// Save user
		User savedUser = userRepository.save(user);

		// Generate JWT token
		String token = jwtUtil.generateToken(savedUser.getUsername());

		// Create response
		UserDTO userDTO = new UserDTO(savedUser);
		return new AuthResponse(token, userDTO);
	}

	public AuthResponse login(LoginRequest request) {
		try {
			// Authenticate user
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getEmailOrUsername(), request.getPassword()));

			// Find user
			User user = userRepository.findByUsernameOrEmail(request.getEmailOrUsername(), request.getEmailOrUsername())
					.orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

			// Generate JWT token
			String token = jwtUtil.generateToken(user.getUsername());

			// Create response
			UserDTO userDTO = new UserDTO(user);
			return new AuthResponse(token, userDTO);

		} catch (BadCredentialsException e) {
			throw new BadCredentialsException("Invalid username/email or password");
		}
	}

	public User getCurrentUser(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}
}