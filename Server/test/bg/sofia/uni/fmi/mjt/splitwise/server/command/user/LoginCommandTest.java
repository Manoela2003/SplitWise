package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.IncorrectPasswordException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserNotFoundException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoginCommandTest {

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new LoginCommand(null, "pass", null);

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenUserIsNotFound()
        throws UserNotFoundException, IncorrectPasswordException, ServerErrorException {
        UserRepository repository = mock(UserRepository.class);

        doThrow(new UserNotFoundException("Incorrect username"))
            .when(repository)
                .loginUser("user1", "pass1");

        Command command = new LoginCommand("user1", "pass1", null)
            .configure(repository, null, null, null);

        assertEquals("Incorrect username", command.execute(),
            "Expected a message: \"Incorrect username\" to be shown but it was not");
    }

    @Test
    void testExecute() throws UserAlreadyExistsException, ServerErrorException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("user", "pass", "test1", "test2");

        SelectionKey key = mock(SelectionKey.class);
        when(key.attach("user")).thenReturn("user");

        Command command = new LoginCommand("user", "pass", key)
            .configure(repository, null, null, null);

        String expectedNotifications = "User user successfully logged in!\nNo notifications to be shown";

        assertEquals(expectedNotifications, command.execute(),
            "Expected user to be logged in successfully");
    }

}
