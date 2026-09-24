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

    public UserService() { this(new UserDAO()); }
    public UserService(UserDAO userDAO) { this.userDAO = userDAO; }

    public User register(String fullName, String email, String phone, String password, String confirmPassword) {
        return registerBuyer(fullName, email, phone, password, confirmPassword);
    }

    public User registerBuyer(String fullName, String email, String phone, String password, String confirmPassword) {
        return registerForRole(fullName, email, phone, password, confirmPassword, UserRole.BUYER);
    }

    public User registerSeller(String fullName, String email, String phone, String password, String confirmPassword) {
        return registerForRole(fullName, email, phone, password, confirmPassword, UserRole.SELLER);
    }

    private User registerForRole(String fullName, String email, String phone, String password,
                                 String confirmPassword, UserRole role) {
        if (role == null || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("This account role cannot be self-registered.");
        }
        validateRegistration(fullName, email, phone, password, confirmPassword);
        String normalizedEmail = normalizeEmail(email);
        if (userDAO.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("Email is already registered.");
        }
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(fullName.trim(), normalizedEmail, passwordHash,
                phone == null ? null : phone.trim(), role, UserStatus.ACTIVE);
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

    public User updateBuyerProfile(Long userId, String fullName, String phone) {
        User user = findActiveBuyer(userId);
        validateProfile(fullName, phone);
        user.setFullName(fullName.trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        userDAO.updateProfile(user.getId(), user.getFullName(), user.getPhone());
        return userDAO.findById(user.getId()).orElse(user);
    }

    public void changeAccountStatus(Long userId, UserStatus status) {
        if (userId == null || status == null) throw new IllegalArgumentException("User id and account status are required.");
        userDAO.updateStatus(userId, status);
        LOGGER.info("Account status changed for user id {}", userId);
    }

    private User findActiveBuyer(Long userId) {
        User user = findById(userId).orElseThrow(() -> new IllegalArgumentException("Buyer account was not found."));
        if (user.getRole() != UserRole.BUYER || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active buyers can update this profile.");
        }
        return user;
    }

    private void validateRegistration(String fullName, String email, String phone, String password, String confirmPassword) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) throw new IllegalArgumentException("Invalid email address.");
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) throw new IllegalArgumentException("Invalid phone number.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must contain at least 6 characters.");
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("Passwords do not match.");
    }

    private void validateProfile(String fullName, String phone) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 120) throw new IllegalArgumentException("Full name is required and must be at most 120 characters.");
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) throw new IllegalArgumentException("Invalid phone number.");
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    public static class AuthenticationException extends RuntimeException { public AuthenticationException(String message) { super(message); } }
    public static class DuplicateEmailException extends RuntimeException { public DuplicateEmailException(String message) { super(message); } }
}
