package bg.sofia.uni.fmi.mjt.splitwise.server.passwords;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.IncorrectPasswordException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordsDatabaseTest {
    PasswordsDatabase database = PasswordsDatabase.getInstance();

    @Test
    void testAddUserCredentialsWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> database.addUserCredentials(null, "pass"),
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testAddUserCredentialsWhenUserExists() throws UserAlreadyExistsException, ServerErrorException {
        database.addUserCredentials("testUsername", "testPass");

        assertThrows(UserAlreadyExistsException.class, () -> database.addUserCredentials("testUsername", "testPass"),
            "Expected UserAlreadyExistsException to be thrown when the user already exists");
    }

    @Test
    void testAddUserCredentials() throws UserAlreadyExistsException, ServerErrorException {
        database.addUserCredentials("test", "testPass");

        assertTrue(database.getUsersPasswords().containsKey("test"),
            "Expected the following user: test to be added to the database");
    }

    @Test
    void testCheckLoginCredentialsWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> database.checkLoginCredentials(null, "pass"),
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testCheckLoginCredentialsWhenUserIsNotFound() {
        assertThrows(UserNotFoundException.class, () -> database.checkLoginCredentials("notFound", "pass"),
            "Expected UserNotFoundException to be thrown when username is null");
    }

    @Test
    void testCheckLoginCredentialsWhenPasswordDoNotMatch() throws UserAlreadyExistsException, ServerErrorException {
        database.addUserCredentials("user1", "password");

        assertThrows(IncorrectPasswordException.class, () -> database.checkLoginCredentials("user1", "pass"),
            "Expected IncorrectPasswordException to be thrown when username is null");
    }
}
