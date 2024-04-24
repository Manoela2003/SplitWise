package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SeeTransactionsCommandTest {
    private static final Path USER_PATH = Path.of("test1_transaction_history.txt");

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(USER_PATH);
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
    }

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new SeeTransactionsCommand(null);

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenThereAreNoTransactions() throws IOException, ServerErrorException {
        Files.deleteIfExists(USER_PATH);

        Command command = new SeeTransactionsCommand("test1");

        assertEquals("There are no transactions", command.execute(),
            "Expected a message: \"There are no transactions\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenThereAreTransactions() throws IOException, UserAlreadyExistsException, ServerErrorException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("test1", "userPass", "userName", "userFamilyName");
        repository.registerUser("test2", "friendPass", "friendName", "friendFamilyName");

        DebtRecord debt = mock(DebtRecord.class);
        when(debt.toString()).thenReturn("test1 test2 10 reason");

        Files.writeString(USER_PATH, debt.toString());

        Command command = new SeeTransactionsCommand("test1")
            .configure(repository, null, null, null);

        String expectedResult = " * Transactions *\n - friendName [test2] approved your 10,00 LV payment for reason\n";
        assertEquals(expectedResult, command.execute(),
            "Expected a transaction to be shown but it was not");
    }
}
