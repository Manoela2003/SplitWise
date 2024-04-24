package bg.sofia.uni.fmi.mjt.splitwise.server.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.notifications.NotificationCenter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UserTest {
    private static final User debtor = new User("Debtor", "DebtorName", "DebtorFamily");
    private static final User creditor = new User("Creditor", "CreditorName", "CreditorFamily");
    private static final DebtRecord debt = new DebtRecord(debtor, creditor, 10, "reason");
    private static NotificationCenter notificationCenter = mock(NotificationCenter.class);
    private static User user = new User("testUsername", "testName", "testName");

    @BeforeAll
    static void setUp() {
        user.setNotificationCenter(notificationCenter);
    }

    @Test
    void testAddToGroupWhenGroupNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> user.addToGroup(null),
            "Expected IllegalArgumentException to be thrown when group name is null but it was not");
    }

    @Test
    void testAddToGroup() {
        user.addToGroup("groupTest1");
        user.addToGroup("groupTest2");

        Set<String> returnedGroups = user.getGroups();

        assertTrue(returnedGroups.contains("groupTest1") && returnedGroups.contains("groupTest2"),
            "Expected user to have the following groups: groupTest1 and groupTest2");
    }

    @Test
    void testIsInGroupWhenGroupNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> user.isInGroup(null),
            "Expected IllegalArgumentException to be thrown when group name is null but it was not");
    }

    @Test
    void testIsInGroupReturnsTrue() {
        user.addToGroup("testGroup");

        assertTrue(user.isInGroup("testGroup"),
            "Expected method to return true when user is part of the following group: testGroup");
    }

    @Test
    void testIsInGroupReturnsFalse() {
        assertFalse(user.isInGroup("test"),
            "Expected method to return false when user is not part of the following group: test");
    }

    @Test
    void testAddPaidDebt() throws ServerErrorException {
        user.addPaidDebt(debt);
        verify(notificationCenter, times(1)).addPaidDebt(debt);
    }

    @Test
    void testAddPartlyPaidDebt() throws ServerErrorException {
        user.addPartlyPaidDebt(debt);
        verify(notificationCenter, times(1)).addPartlyPaidDebt(debt);
    }

    @Test
    void testAddNewDebt() throws ServerErrorException {
        user.addNewDebt(debt);
        verify(notificationCenter, times(1)).addNewDebt(debt);
    }

    @Test
    void testAddNewGroupDebt() throws ServerErrorException {
        user.addNewGroupDebt(debt, "group");
        verify(notificationCenter, times(1)).addNewGroupDebt(debt, "group");
    }

    @Test
    void testGetNotifications() throws ServerErrorException {
        user.getNotifications();
        verify(notificationCenter, times(1)).getNotifications();
    }

    @Test
    void testConstructorWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new User(null, "name", "family"),
            "Expected IllegalArgumentException to be thrown when username is null");
    }
}
