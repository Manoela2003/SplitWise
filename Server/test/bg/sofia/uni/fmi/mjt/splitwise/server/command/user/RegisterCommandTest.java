package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RegisterCommandTest {

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new RegisterCommand(null, "pass", "test1", "test2", null);

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenUserAlreadyExists() throws UserAlreadyExistsException, ServerErrorException {
        UserRepository repository = mock(UserRepository.class);

        doThrow(new UserAlreadyExistsException("The username already exists"))
            .when(repository)
            .registerUser("user", "pass", "test1", "test2");

        Command command = new RegisterCommand("user", "pass", "test1", "test2", null)
            .configure(repository, null, null, null);

        assertEquals("The username already exists", command.execute(),
            "Expected a message: \"The username already exists\" to be shown but it was not");
    }

    @Test
    void testExecute() throws ServerErrorException, UserAlreadyExistsException {
        UserRepository repository = mock(UserRepository.class);
        SelectionKey key = mock(SelectionKey.class);

        Command command = new RegisterCommand("user", "pass", "test1", "test2", key)
            .configure(repository, null, null, null);

        assertEquals("User user successfully registered!", command.execute(),
            "Expected a message: \"The username already exists\" to be shown but it was not");

        verify(repository, times(1))
            .registerUser("user", "pass", "test1", "test2");
    }
}
