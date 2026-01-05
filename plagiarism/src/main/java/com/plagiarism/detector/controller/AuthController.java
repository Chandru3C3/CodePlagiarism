package com.plagiarism.detector.controller;
import com.plagiarism.detector.dto.*;
import com.plagiarism.detector.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cpd/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(400, "Registration Failed", e.getMessage(), "/api/auth/register"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Authentication Failed", e.getMessage(), "/api/auth/login"));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            var user = authService.getCurrentUser(username);
            UserDTO userDTO = new UserDTO(user);
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "User Not Found", e.getMessage(), "/api/auth/me"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT tokens are stateless, so logout is handled on the client side
        return ResponseEntity.ok().body("Logged out successfully");
    }
}