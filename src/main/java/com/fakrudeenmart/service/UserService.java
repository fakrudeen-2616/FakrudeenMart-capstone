package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,30}$");
    private final UserDAO userDAO;

    public UserService() {
        this(new UserDAO());
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User register(String fullName, String email, String phone, String password, String confirmPassword) {
        validateRegistration(fullName, email, phone, password, confirmPassword);
        String normalizedEmail = normalizeEmail(email);
        if (userDAO.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("Email is already registered.");
        }
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(fullName.trim(), normalizedEmail, passwordHash,
                phone == null ? null : phone.trim(), UserRole.BUYER, UserStatus.ACTIVE);
        return userDAO.createUser(user);
    }

    public User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }
        Optional<User> candidate = userDAO.findByEmail(normalizeEmail(email));
        if (candidate.isEmpty() || candidate.get().getPasswordHash() == null
                || !BCrypt.checkpw(password, candidate.get().getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }
        User user = candidate.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Your account is inactive. Please contact the administrator.");
        }
        return user;
    }

    public Optional<User> findByEmail(String email) {
        return email == null || email.isBlank() ? Optional.empty() : userDAO.findByEmail(normalizeEmail(email));
    }

    public Optional<User> findById(Long id) {
        return id == null ? Optional.empty() : userDAO.findById(id);
    }

    public User updateProfile(Long userId, String fullName, String phone) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("User id is required.");
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 120) {
            throw new IllegalArgumentException("Full name is required and must be at most 120 characters.");
        }
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Invalid phone number.");
        }
        userDAO.updateProfile(userId, fullName.trim(), phone == null ? "" : phone.trim());
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("User was not found."));
    }

    public void changeAccountStatus(Long userId, UserStatus status) {
        if (userId == null || status == null) {
            throw new IllegalArgumentException("User id and account status are required.");
        }
        userDAO.updateStatus(userId, status);
        LOGGER.info("Account status changed for user id {}", userId);
    }

    private void validateRegistration(String fullName, String email, String phone,
                                      String password, String confirmPassword) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Invalid phone number.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) { super(message); }
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String message) { super(message); }
    }
}
