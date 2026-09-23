package com.fakrudeenmart;

import com.fakrudeenmart.dao.UserDAO;
import com.fakrudeenmart.model.User;
import com.fakrudeenmart.model.UserRole;
import com.fakrudeenmart.model.UserStatus;
import com.fakrudeenmart.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserDAO userDAO;

    @Test
    void validRegistrationCreatesActiveBuyerWithBCryptHash() {
        when(userDAO.findByEmail("buyer@example.com")).thenReturn(Optional.empty());
        when(userDAO.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = new UserService(userDAO).register("Buyer One", "BUYER@example.com", "+123456789", "secret1", "secret1");

        assertEquals(UserRole.BUYER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotEquals("secret1", user.getPasswordHash());
        assertTrue(BCrypt.checkpw("secret1", user.getPasswordHash()));
        verify(userDAO).createUser(any(User.class));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userDAO.findByEmail("buyer@example.com")).thenReturn(Optional.of(existingUser()));
        assertThrows(UserService.DuplicateEmailException.class,
                () -> new UserService(userDAO).register("Buyer One", "buyer@example.com", "", "secret1", "secret1"));
    }

    @Test
    void mismatchedPasswordsAndInvalidInputAreRejected() {
        UserService service = new UserService(userDAO);
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Buyer", "buyer@example.com", "", "secret1", "different"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Buyer", "not-an-email", "", "secret1", "secret1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Buyer", "buyer@example.com", "bad", "secret1", "secret1"));
    }

    @Test
    void correctCredentialsReturnTheStoredUserAndRole() {
        User user = existingUser();
        when(userDAO.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        User authenticated = new UserService(userDAO).authenticate("BUYER@example.com", "secret1");
        assertEquals(UserRole.BUYER, authenticated.getRole());
    }

    @Test
    void wrongPasswordUnknownEmailAndInactiveAccountAreRejected() {
        User user = existingUser();
        when(userDAO.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        UserService service = new UserService(userDAO);
        assertThrows(UserService.AuthenticationException.class, () -> service.authenticate("buyer@example.com", "wrong"));
        when(userDAO.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertThrows(UserService.AuthenticationException.class, () -> service.authenticate("unknown@example.com", "secret1"));
        user.setStatus(UserStatus.INACTIVE);
        assertThrows(UserService.AuthenticationException.class, () -> service.authenticate("buyer@example.com", "secret1"));
    }

    private User existingUser() {
        User user = new User("Buyer One", "buyer@example.com", BCrypt.hashpw("secret1", BCrypt.gensalt()),
                "+123456789", UserRole.BUYER, UserStatus.ACTIVE);
        user.setId(1L);
        return user;
    }
}
