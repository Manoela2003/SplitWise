package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetStatusCommandTest {
    private static UserRepository repository;

    @BeforeAll
    static void setUp() throws UserAlreadyExistsException, ServerErrorException {
        repository = UserRepository.getInstance();
        repository.registerUser("userTest", "pass", "test1", "test2");
    }

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new GetStatusCommand(null);

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenThereAreNoNotifications() throws ServerErrorException {
        DebtManager manager = mock(DebtManager.class);
        Command command = new GetStatusCommand("userTest")
            .configure(repository, null, manager, null);

        assertEquals("No notifications to be shown", command.execute(),
            "Expected a message: \"No notifications to be shown\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenThereAreDebts() throws ServerErrorException {
        DebtRecord debt = mock(DebtRecord.class);
        when(debt.visualizeDebt("Owes you")).thenReturn("UserName UserFamily (user) Owes you 15 LV [Party]");

        Set<DebtRecord> moneyOwed = new HashSet<>();
        moneyOwed.add(debt);

        DebtManager manager = mock(DebtManager.class);
        when(manager.getMoneyOwed("userTest")).thenReturn(moneyOwed);

        Command command = new GetStatusCommand("userTest")
            .configure(repository, null, manager, null);

        String expectedNotifications = " * Friends:\n - " + debt.visualizeDebt("Owes you") + '\n';

        assertEquals(expectedNotifications, command.execute(),
            "Expected friend to owe debt");
    }

    @Test
    void testExecuteWhenThereAreGroupDebts() throws ServerErrorException {
        DebtRecord debt = mock(DebtRecord.class);
        when(debt.visualizeDebt("You owe")).thenReturn("UserName UserFamily (user) You owe 15 LV [Party]");

        Set<DebtRecord> owesMoney = new HashSet<>();
        owesMoney.add(debt);

        UserRepository.toUser("userTest").addToGroup("group");

        DebtManager manager = mock(DebtManager.class);
        when(manager.getGroupOwesMoney("group", "userTest")).thenReturn(owesMoney);

        Command command = new GetStatusCommand("userTest")
            .configure(repository, null, manager, null);

        String expectedNotifications = "\n * Group: group\n - " + debt.visualizeDebt("You owe") + '\n';

        assertEquals(expectedNotifications, command.execute(),
            "Expected userTest to owe one debt");
    }
}
