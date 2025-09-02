package com.example.projectplanner.controller;

import com.example.projectplanner.model.User;
import com.example.projectplanner.security.JwtResponse;
import com.example.projectplanner.service.UserService;
import com.example.projectplanner.service.VerificationService;
import com.example.projectplanner.repository.UserRepository;
import com.example.projectplanner.security.JwtProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, UserRepository userRepository, JwtProvider jwtProvider, VerificationService verificationService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.verificationService = verificationService;
        this.passwordEncoder = passwordEncoder;
    }


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User newUser = userService.registerUser(
                user.getName(),
                user.getEmail(),
                user.getPassword()
        );

        verificationService.sendVerificationEmail(newUser);

        return ResponseEntity.ok(Map.of("message", "Registration successful. Please check your email for verification."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest) {
        return userService.findByEmail(loginRequest.getEmail())
                .map(user -> {
                    if (user.getStatus() == 0) {
                        return ResponseEntity.status(400).body("Email not verified");
                    } else if (user.getStatus() == 2) {
                        return ResponseEntity.status(400).body("Account has been deleted");
                    }

                    if (userService.matchesPassword(loginRequest.getPassword(), user.getPassword())) {
                        String role = user.getRole().name();

                        String token = jwtProvider.generateToken(user.getEmail(), List.of(role), user.getId());

                        return ResponseEntity.ok(new JwtResponse(token));
                    } else {
                        return ResponseEntity.status(401).body("Invalid credentials");
                    }
                })
                .orElse(ResponseEntity.status(404).body("User not found"));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        if (verificationService.verifyToken(token)) {
            return ResponseEntity.ok("Email successfully verified");
        }
        return ResponseEntity.status(400).body("Invalid or expired token");
    }


    @PutMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            User existingUser = user.get();
            existingUser.setStatus(2); // Mark as deleted
            existingUser.setDeletedDate(LocalDateTime.now()); // Set deleted date
            userRepository.save(existingUser);
            return ResponseEntity.ok("User marked as deleted.");
        }
        return ResponseEntity.status(404).body("User not found.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        Optional<User> userOptional = userService.findByEmail(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOptional.get();
        verificationService.sendPasswordResetEmail(user);
        return ResponseEntity.ok("Password reset email sent");
    }



    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String token, @RequestBody Map<String, String> requestBody) {
        String newPassword = requestBody.get("newPassword");

        if (!verificationService.verifyPasswordResetToken(token)) {
            return ResponseEntity.status(400).body("Invalid or expired token.");
        }

        User user = verificationService.getUserByResetToken(token);
        if (user != null) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);

            verificationService.deletePasswordResetToken(token);
            return ResponseEntity.ok("Password successfully reset.");
        }
        return ResponseEntity.status(404).body("User not found.");
    }

}