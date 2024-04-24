package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.GroupAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SplitGroupCommandTest {
    private final GroupManager manager = mock(GroupManager.class);

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("groups.txt"));
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new SplitGroupCommand(null, "group", "10", "reason");

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenAmountIsNotANumber() throws ServerErrorException {
        Command command = new SplitGroupCommand("user", "group", "notANumber", "reason");

        assertEquals("The amount should be a number", command.execute(),
            "Expected a message showing that the amount should be a number");
    }

    @Test
    void testExecuteWhenGroupDoNotExist() throws ServerErrorException {
        when(manager.existsGroup("groupName")).thenReturn(false);

        Command command = new SplitGroupCommand("user", "groupName", "10", "reason")
            .configure(null, manager, null, null);

        assertEquals("Group groupName doesn't exist", command.execute(),
            "Expected group groupName to don't exist");
    }

    @Test
    void testExecuteWhenUserIsNotPartOfTheGroup() throws UserAlreadyExistsException, ServerErrorException {
        when(manager.existsGroup("groupName")).thenReturn(true);

        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("userTester1", "userPass1", "userName", "userFamily");

        Command command = new SplitGroupCommand("userTester1", "groupName", "10", "reason")
            .configure(repository, manager, null, null);

        assertEquals("You are not part of the following group: groupName", command.execute(),
            "Expected userTester1 to not be part of groupName");
    }

    @Test
    void testExecuteWhenAmountIsNonPositive()
        throws UserAlreadyExistsException, ServerErrorException, GroupAlreadyExistsException,
        NonPositiveAmountException {
        GroupManager groupManager = GroupManager.getInstance();

        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("userTest1", "userPass1", "userName", "userFamily");
        repository.registerUser("userTest2", "userPass2", "userName", "userFamily");
        repository.registerUser("userTest3", "userPass3", "userName", "userFamily");

        groupManager.addGroup("groupName", Set.of("userTest1", "userTest2", "userTest3"));

        DebtManager debtManager = mock(DebtManager.class);
        doThrow(new NonPositiveAmountException("The amount cannot be 0 or less"))
            .when(debtManager)
            .splitGroupBill(GroupManager.toGroup("groupName"), "userTest1", -15, "reason");

        Command command = new SplitGroupCommand("userTest1", "groupName", "-15", "reason")
            .configure(repository, groupManager, debtManager, null);

        assertEquals("The amount cannot be 0 or less", command.execute(),
            "Expected a message showing that the amount cannot be non positive");
    }

    @Test
    void testExecute() throws ServerErrorException, GroupAlreadyExistsException, UserAlreadyExistsException {
        GroupManager groupManager = GroupManager.getInstance();

        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("testUsername1", "userPass1", "userName", "userFamily");
        repository.registerUser("testUsername2", "userPass2", "userName", "userFamily");
        repository.registerUser("testUsername3", "userPass3", "userName", "userFamily");

        groupManager.addGroup("groupTestName", Set.of("testUsername1", "testUsername2", "testUsername3"));

        DebtManager debtManager = mock(DebtManager.class);

        Command command = new SplitGroupCommand("testUsername1", "groupTestName", "30", "reason")
            .configure(repository, groupManager, debtManager, null);

        assertEquals("Amount successfully split", command.execute(),
            "Expected a message showing that the amount is successfully split");
    }
}
