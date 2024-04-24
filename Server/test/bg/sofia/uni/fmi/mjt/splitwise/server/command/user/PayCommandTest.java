package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NoDebtsToBePaidException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PayCommandTest {
    private final UserRepository repository = mock(UserRepository.class);

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("friend_transaction_history.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new PayCommand(null, "friend", "15");

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenUserDoNotExist() throws ServerErrorException {
        when(repository.containsUser("friend")).thenReturn(false);

        Command command = new PayCommand("user", "friend", "10")
            .configure(repository, null, null, null);

        assertEquals("The following user doesn't exist: friend", command.execute(),
            "Expected a message: \"The following user doesn't exist: friend\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenUserIsTheSameAsFriend() throws ServerErrorException {
        when(repository.containsUser("user")).thenReturn(true);

        Command command = new PayCommand("user", "user", "10")
            .configure(repository, null, null, null);

        assertEquals("You cannot pay the bill yourself", command.execute(),
            "Expected a message: \"You cannot pay the bill yourself\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenAmountIsNotANumber() throws ServerErrorException {
        when(repository.containsUser("friend")).thenReturn(true);

        Command command = new PayCommand("user", "friend", "notANumber")
            .configure(repository, null, null, null);

        assertEquals("The amount should be a number", command.execute(),
            "Expected a message: \"The amount should be a number\" to be shown but it was not");
    }

    @Test
    void testExecute() throws ServerErrorException, NonPositiveAmountException, NoDebtsToBePaidException {
        DebtManager manager = mock(DebtManager.class);
        DebtRecord debt = mock(DebtRecord.class);

        when(debt.reason()).thenReturn("reason");
        when(debt.amount()).thenReturn(Double.parseDouble("20"));
        when(repository.containsUser("friend")).thenReturn(true);

        Set<DebtRecord> debts = new HashSet<>(Set.of(debt));

        when(manager.pay("user", 20, "friend", new HashSet<>())).thenReturn(debts);

        Command command = new PayCommand("user", "friend", "20")
            .configure(repository, null, manager, null);

        String expectedNotifications = " * friend successfully paid for:\n - reason 20.0\n";

        assertEquals(expectedNotifications, command.execute(),
            "Expected a text notifying that friend paid successfully");
    }

    @Test
    void testExecuteWhenAmountIsNonPositive() throws ServerErrorException, NonPositiveAmountException, NoDebtsToBePaidException {
        DebtManager manager = mock(DebtManager.class);
        when(repository.containsUser("friend")).thenReturn(true);

        doThrow(new NonPositiveAmountException("The amount cannot be 0 or less"))
            .when(manager)
            .pay("user", -10, "friend", new HashSet<>());

        Command command = new PayCommand("user", "friend", "-10")
            .configure(repository, null, manager, null);

        assertEquals("The amount cannot be 0 or less", command.execute(),
            "Expected a message: \"The amount cannot be 0 or less\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenThereAreNoDebts() throws ServerErrorException, NonPositiveAmountException, NoDebtsToBePaidException {
        DebtManager manager = mock(DebtManager.class);
        when(repository.containsUser("friend")).thenReturn(true);

        doThrow(new NoDebtsToBePaidException("Friend doesn't owe you anything"))
            .when(manager)
            .pay("user", 10, "friend", new HashSet<>());

        Command command = new PayCommand("user", "friend", "10")
            .configure(repository, null, manager, null);

        assertEquals("Friend doesn't owe you anything", command.execute(),
            "Expected a message: \"Friend doesn't owe you anything\" to be shown but it was not");
    }
}
