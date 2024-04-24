package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.FriendAlreadyAddedException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.friends.FriendsManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddFriendCommandTest {
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() throws UserAlreadyExistsException, ServerErrorException {
        userRepository = UserRepository.getInstance();
        userRepository.registerUser("friend", "pass", "test1", "test1");
    }

    @AfterEach
    void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new AddFriendCommand(null, "friend");

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when the username is null");
    }

    @Test
    void testExecuteWhenUsernameAndFriendNameAreEqual() throws ServerErrorException {
        Command command = new AddFriendCommand("user", "user");

        assertEquals("You cannot add yourself as friend", command.execute(),
            "Expected a message: \"You cannot add yourself as friend\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenFriendDoNotExist() throws ServerErrorException {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.containsUser("friend")).thenReturn(false);
        Command command = new AddFriendCommand("user", "friend")
            .configure(userRepository, null, null, null);

        assertEquals("User friend doesn't exist", command.execute(),
            "Expected a message: \"User friend doesn't exist\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenAddFriendAlreadyExists()
        throws ServerErrorException, FriendAlreadyAddedException {
        FriendsManager friendsManager = mock(FriendsManager.class);

        Command command = new AddFriendCommand("user", "friend")
            .configure(userRepository, null, null, friendsManager);

        doThrow(new FriendAlreadyAddedException("User friend is already in your friend list"))
            .when(friendsManager)
                .addFriend("user", UserRepository.toUser("friend"));

        assertEquals("User friend is already in your friend list", command.execute(),
            "Expected a message: \"User friend is already in your friend list\" to be shown but it was not");
    }

    @Test
    void testExecute() throws ServerErrorException, FriendAlreadyAddedException {
        FriendsManager friendsManager = mock(FriendsManager.class);

        Command command = new AddFriendCommand("user", "friend")
            .configure(userRepository, null, null, friendsManager);

        assertEquals("User friend is successfully added to your friend list", command.execute(),
            "Expected a message: \"User friend is successfully added to your friend list\" to be shown but it was not");

        verify(friendsManager, times(1)).addFriend("user", UserRepository.toUser("friend"));
    }
}
