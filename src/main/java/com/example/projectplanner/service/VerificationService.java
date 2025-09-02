package com.example.projectplanner.service;

import com.example.projectplanner.model.PasswordResetToken;
import com.example.projectplanner.model.User;
import com.example.projectplanner.model.VerificationToken;
import com.example.projectplanner.repository.PasswordResetTokenRepository;
import com.example.projectplanner.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VerificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    public void sendVerificationEmail(User user) {
        String token = generateVerificationToken(user);
        String verificationLink = "http://localhost:4200/verify?token=" + token;
        // String verificationLink = "http://localhost:8080/auth/verify?token=" + token;

        // Send verification email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Email Verification");
        message.setText("Click the following link to verify your email: " + verificationLink);
        mailSender.send(message);
    }

    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(
                token, user, LocalDateTime.now().plusHours(24)
        );
        tokenRepository.save(verificationToken);
        return token;
    }

    public boolean verifyToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken != null && verificationToken.getExpirationDate().isAfter(LocalDateTime.now())) {
            User user = verificationToken.getUser();
            user.setStatus(1);
            userService.saveUser(user);
            return true;
        }
        return false;
    }

    public void sendPasswordResetEmail(User user) {
        String token = generatePasswordResetToken(user);
        // String resetLink = "http://localhost:8080/auth/reset-password?token=" + token;
        String resetLink = "http://localhost:4200/reset-password?token=" + token;
        // Send password reset email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Reset Request");
        message.setText("Click the following link to reset your password: " + resetLink);
        mailSender.send(message);
    }

    @Transactional
    public String generatePasswordResetToken(User user) {
        resetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                token, user, LocalDateTime.now().plusHours(1) // Expires in 1 hour
        );

        resetTokenRepository.save(resetToken); // Save new token
        return token;
    }

    public boolean verifyPasswordResetToken(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token);

        if (resetToken == null) {
            System.out.println("Token not found in database"); // Debugging
            return false;
        }

        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            System.out.println("Token is expired"); // Debugging
            return false;
        }

        return true;
    }

    public User getUserByResetToken(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token);
        return (resetToken != null) ? resetToken.getUser() : null;
    }

    @Transactional
    public void deletePasswordResetToken(String token) {
        resetTokenRepository.deleteByToken(token);
    }


    @Transactional
    public void deleteByUser(User user) {
        resetTokenRepository.deleteByUser(user);
    }
}