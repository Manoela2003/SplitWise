package bg.sofia.uni.fmi.mjt.splitwise.server.friends;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.FriendAlreadyAddedException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FriendsManagerTest {
    private final FriendsManager manager = FriendsManager.getInstance();
    private static User friend;

    @BeforeAll
    static void setUp() {
        friend = mock(User.class);
        when(friend.getUsername()).thenReturn("friend");
    }

    @AfterEach
    void clearFile() throws IOException {
        Files.deleteIfExists(Path.of("usersFriends.txt"));
    }

    @Test
    void testAddFriendWhenFriendIsNull() {
        assertThrows(IllegalArgumentException.class, () -> manager.addFriend("user", null),
            "Expected IllegalArgumentException when friend is null");
    }

    @Test
    void testAddFriendWhenFriendIsAlreadyAdded() throws FriendAlreadyAddedException, ServerErrorException {
        User test = mock(User.class);
        when(test.getUsername()).thenReturn("test");
        manager.addFriend("user", test);

        assertThrows(FriendAlreadyAddedException.class, () -> manager.addFriend("user", test),
            "Expected FriendAlreadyAddedException when friend is already added");
    }

    @Test
    void testHasFriendWhenFriendIsNull() {
        assertThrows(IllegalArgumentException.class, () -> manager.hasFriend("user", null),
            "Expected IllegalArgumentException when friend is null");
    }

    @Test
    void testHasFriendReturnsTrue() throws FriendAlreadyAddedException, ServerErrorException {
        manager.addFriend("user", friend);

        assertTrue(manager.hasFriend("user", friend),
            "Expected true to be returned when user has the following friend: friend");
    }

    @Test
    void testHasFriendReturnsFalse() {
        assertFalse(manager.hasFriend("test", friend),
            "Expected false to be returned when there is no such user");
    }

    @Test
    void testInitialize() throws UserAlreadyExistsException, ServerErrorException, IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("friendTest1", "testPass", "userName", "userFamilyName");
        Files.writeString(Path.of("usersFriends.txt"), "user friendTest1" + System.lineSeparator());

        manager.initialize();

        assertTrue(manager.hasFriend("user", UserRepository.toUser("friendTest1")),
            "Expected friends to be successfully loaded into the friends manager");
    }
}
