package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.friends.FriendsManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SplitCommandTest {
    private final UserRepository repository = mock(UserRepository.class);

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new SplitCommand(null, "friend", "10", "reason");

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenAmountIsNotANumber() throws ServerErrorException {
        Command command = new SplitCommand("user", "friend", "notANumber", "reason");

        assertEquals("The amount should be a number", command.execute(),
            "Expected a message: \"The amount should be a number\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenFriendDoNotExist() throws ServerErrorException {
        when(repository.containsUser("friend")).thenReturn(false);
        Command command = new SplitCommand("user", "friend", "10", "reason")
            .configure(repository, null, null, null);

        assertEquals("The following user doesn't exist: friend", command.execute(),
            "Expected a message: \"The following user doesn't exist: friend\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenUserEqualsFriend() throws ServerErrorException {
        when(repository.containsUser("friend")).thenReturn(true);

        Command command = new SplitCommand("friend", "friend", "10", "reason")
            .configure(repository, null, null, null);

        assertEquals("You cannot split the bill with yourself", command.execute(),
            "Expected a message: \"You cannot split the bill with yourself\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenUserDoNotHaveFriend() throws UserAlreadyExistsException, ServerErrorException {
        UserRepository repo = UserRepository.getInstance();
        repo.registerUser("tester", "testerPass", "testerName", "testerFamily");

        FriendsManager friendsManager = mock(FriendsManager.class);
        when(friendsManager.hasFriend("user", UserRepository.toUser("tester"))).thenReturn(false);

        Command command = new SplitCommand("user", "tester", "20", "reason")
            .configure(repo, null, null, friendsManager);

        assertEquals("You don't have tester in your friend list. Add him first in order to split your bill.",
            command.execute(), "Expected user to not have tester as his friend");
    }

    @Test
    void testExecuteWhenAmountIsNonPositive()
        throws UserAlreadyExistsException, ServerErrorException, NonPositiveAmountException {
        UserRepository repo = UserRepository.getInstance();
        repo.registerUser("tester1", "testerPass", "testerName", "testerFamily");

        FriendsManager friendsManager = mock(FriendsManager.class);
        when(friendsManager.hasFriend("user", UserRepository.toUser("tester1"))).thenReturn(true);

        DebtManager debtManager = mock(DebtManager.class);
        doThrow(new NonPositiveAmountException("The amount cannot be 0 or less"))
            .when(debtManager)
            .splitBill("user", "tester1", -10, "reason");

        Command command = new SplitCommand("user", "tester1", "-10", "reason")
            .configure(repo, null, debtManager, friendsManager);

        assertEquals("The amount cannot be 0 or less", command.execute(),
            "Expected a message: \"The amount cannot be 0 or less\" to be shown but it was not");
    }

    @Test
    void testExecute() throws ServerErrorException, UserAlreadyExistsException {
        UserRepository repo = UserRepository.getInstance();
        repo.registerUser("testerName", "testerPass", "testerName", "testerFamily");

        FriendsManager friendsManager = mock(FriendsManager.class);
        when(friendsManager.hasFriend("user", UserRepository.toUser("testerName"))).thenReturn(true);

        DebtManager debtManager = mock(DebtManager.class);

        Command command = new SplitCommand("user", "testerName", "15", "reason")
            .configure(repo, null, debtManager, friendsManager);

        assertEquals("Split 15.0 between you and testerName", command.execute(),
            "Expected a message: \"Split 15.0 between you and testerName\" to be shown but it was not");
    }
}
