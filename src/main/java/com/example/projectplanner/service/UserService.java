package com.example.projectplanner.service;

import java.time.LocalDateTime;

import com.example.projectplanner.model.Role;
import com.example.projectplanner.model.User;
import com.example.projectplanner.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String name, String email, String password) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        user.setRole(Role.USER);
        user.setStatus(0); // unverified
        user.setAddedDate(LocalDateTime.now()); // Set registration date
        user.setDeletedDate(null); // Not deleted

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void saveUser(User user) {
        userRepository.save(user);  // Save the user in the database
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void softDeleteUser(Long userId) {
        User user = getUserById(userId);
        user.softDelete();
        userRepository.save(user);
    }

    public void makeAdmin(Long userId) {
        User user = getUserById(userId);
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }


    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


}