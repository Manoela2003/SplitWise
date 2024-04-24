package bg.sofia.uni.fmi.mjt.splitwise.server.notifications;

import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NotificationCenterTest {
    private final NotificationCenter notificationCenter = new NotificationCenter("User");
    private static DebtRecord debt;

    @BeforeAll
    static void setUp() throws UserAlreadyExistsException, ServerErrorException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("User", "pass", "test1", "test1");
        repository.registerUser("Creditor", "pass", "test2", "test2");

        debt = new DebtRecord(UserRepository.toUser("User"), UserRepository.toUser("Creditor"),
            15, "Drinks");
    }

    @AfterEach
    void clearFile() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
    }

    @Test
    void testAddPaidDebtWhenDebtIsNull() {
        assertThrows(IllegalArgumentException.class, () -> notificationCenter.addPaidDebt(null),
            "Expected IllegalArgumentException to be thrown when debt is null");
    }

    @Test
    void testAddPartlyPaidDebtWhenDebtIsNull() {
        assertThrows(IllegalArgumentException.class, () -> notificationCenter.addPartlyPaidDebt(null),
            "Expected IllegalArgumentException to be thrown when debt is null");
    }

    @Test
    void testAddNewDebtWhenDebtIsNull() {
        assertThrows(IllegalArgumentException.class, () -> notificationCenter.addNewDebt(null),
            "Expected IllegalArgumentException to be thrown when debt is null");
    }

    @Test
    void testAddNewGroupDebtWhenDebtIsNull() {
        assertThrows(IllegalArgumentException.class, () -> notificationCenter.addNewGroupDebt(null, "group"),
            "Expected IllegalArgumentException to be thrown when debt is null");
    }

    @Test
    void testGetNotificationsWhenThereAreNone() throws ServerErrorException {
        assertEquals("No notifications to be shown", notificationCenter.getNotifications(),
            "Expected no notifications to be shown when there aren't any");
    }

    @Test
    void testGetNotificationsWhenThereArePaidDebts() throws ServerErrorException {
        notificationCenter.addPaidDebt(debt);

        assertEquals("*** Notifications ***\n * Successfully paid debts:\n" + debt.getSuccessfullyPaidDebt() + '\n',
            notificationCenter.getNotifications(),
            "Expected successfully paid debts to be shown but they weren't");
    }

    @Test
    void testGetNotificationsWhenThereArePartlyPaidDebts() throws ServerErrorException {
        notificationCenter.addPartlyPaidDebt(debt);

        assertEquals("*** Notifications ***\n * Partly paid debts:\n" + debt.getPartlyPaidDebt() + '\n',
            notificationCenter.getNotifications(),
            "Expected successfully paid debts to be shown but they weren't");
    }

    @Test
    void testGetNotificationsWhenThereAreNewDebts() throws ServerErrorException {
        notificationCenter.addNewDebt(debt);

        assertEquals("*** Notifications ***\n * New friend debts:\n - " + debt.visualizeDebt("You owe") + '\n',
            notificationCenter.getNotifications(),
            "Expected successfully paid debts to be shown but they weren't");
    }

    @Test
    void testGetNotificationsWhenThereAreNewGroupDebts() throws ServerErrorException {
        notificationCenter.addNewGroupDebt(debt, "testGroup");

        assertEquals("*** Notifications ***\n * New group debts:\n ** Group: testGroup\n - "
                + debt.visualizeDebt("You owe") + '\n', notificationCenter.getNotifications(),
            "Expected successfully paid debts to be shown but they weren't");
    }
}
