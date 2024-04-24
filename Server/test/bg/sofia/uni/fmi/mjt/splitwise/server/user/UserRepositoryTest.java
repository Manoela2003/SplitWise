package bg.sofia.uni.fmi.mjt.splitwise.server.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.IncorrectPasswordException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserNotFoundException;
import bg.sofia.uni.fmi.mjt.splitwise.server.passwords.PasswordsDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UserRepositoryTest {
    private final PasswordsDatabase database = mock(PasswordsDatabase.class);
    private final UserRepository repository = UserRepository.setInstance(database);

    @AfterEach
    void clearFile() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
    }

    @Test
    void testToUserWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> UserRepository.toUser(null),
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testToUser() throws UserAlreadyExistsException, ServerErrorException {
        User user = new User("test", "testName", "testFamily");
        repository.registerUser("test", "testPass", "testName", "testFamily");

        assertEquals(user.getUsername(), UserRepository.toUser("test").getUsername(),
            "Expected user with username test to be returned");
    }

    @Test
    void testRegisterUserWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> repository.registerUser(null, "test", "test", "test"),
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testRegisterUserSuccess() throws UserAlreadyExistsException, ServerErrorException {
        repository.registerUser("username", "password", "firstName", "familyName");

        assertTrue(repository.containsUser("username"),
            "Expected method register user to successfully add user");

        verify(database, times(1)).addUserCredentials("username", "password");
    }

    @Test
    void testLoginUser() throws UserNotFoundException, IncorrectPasswordException, ServerErrorException {
        repository.loginUser("test", "testPass");

        verify(database, times(1)).checkLoginCredentials("test", "testPass");
    }

    @Test
    void testContainsUserWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.containsUser(null),
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testContainsUserReturnsTrue() throws UserAlreadyExistsException, ServerErrorException {
        repository.registerUser("test", "testPass", "testName", "testFamily");

        assertTrue(repository.containsUser("test"),
            "Expected containsUser to return true when searching for username: test");
    }

    @Test
    void testContainsUserReturnsFalse() {
        assertFalse(repository.containsUser("notFound"),
            "Expected containsUser to return false when searching for username: test");
    }

    @Test
    void testInitialize() throws ServerErrorException {
        repository.initialize();
        verify(database, times(1)).initialize();
    }
}
