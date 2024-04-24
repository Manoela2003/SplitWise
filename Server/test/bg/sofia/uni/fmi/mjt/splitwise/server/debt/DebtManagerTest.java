package bg.sofia.uni.fmi.mjt.splitwise.server.debt;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.GroupAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NoDebtsToBePaidException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.Group;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DebtManagerTest {
    private final DebtManager debtManager = DebtManager.getInstance();
    private final GroupManager groupManager = GroupManager.getInstance();

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
        Files.deleteIfExists(Path.of("testingUser_debts.txt"));
        Files.deleteIfExists(Path.of("GroupName_group_debts.txt"));
    }

    @Test
    void testSplitBillWhenAmountIsNonPositive() {
        assertThrows(NonPositiveAmountException.class,
            () -> debtManager.splitBill("user1", "user2", -10, "reason"),
            "Expected NonPositiveAmountException to be thrown when the amount is a non positive number");
    }

    @Test
    void testSplitGroupBillWhenAmountIsNonPositive() {
        Group group = mock(Group.class);
        assertThrows(NonPositiveAmountException.class, () -> debtManager.splitGroupBill(group, "creditor", -10, "user"),
            "Expected NonPositiveAmountException to be thrown when amount is non positive");
    }

    @Test
    void testInitializeWithFriendDebts() throws UserAlreadyExistsException, ServerErrorException, IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("testingUser", "userPass", "name", "family");
        repository.registerUser("testingUserFriend", "userPass", "name", "family");

        DebtRecord debt = mock(DebtRecord.class);
        when(debt.toString()).thenReturn("testingUser testingUserFriend 15.0 reason");
        Files.writeString(Path.of("testingUser_debts.txt"), debt.toString());

        debtManager.initialize();

        String owesMoneyResponse = debtManager.getOwesMoney("testingUser").stream()
            .findFirst().get().toString();
        String moneyOwedResponse = debtManager.getMoneyOwed("testingUserFriend").stream()
            .findFirst().get().toString();

        assertEquals(debt.toString(), owesMoneyResponse,
            "Expected testingUser to have a debt towards testingUserFriend");

        assertEquals(debt.toString(), moneyOwedResponse,
            "Expected testingUser to have a debt towards testingUserFriend");

    }

    @Test
    void testInitializeWithGroupDebts()
        throws UserAlreadyExistsException, ServerErrorException, IOException, GroupAlreadyExistsException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("userTesting1", "userPass", "name", "family");
        repository.registerUser("friendTesting1", "userPass", "name", "family");
        repository.registerUser("memberTesting1", "userPass", "name", "family");

        groupManager.addGroup("GroupName", Set.of("userTesting1", "friendTesting1", "memberTesting1"));

        DebtRecord debt = mock(DebtRecord.class);
        when(debt.toString()).thenReturn("GroupName userTesting1 friendTesting1 15.0 reason");

        Files.writeString(Path.of("GroupName_group_debts.txt"), debt.toString());

        debtManager.initialize();

        String groupMoneyOwed = debtManager.getGroupMoneyOwed("GroupName", "friendTesting1").stream()
            .findFirst().get().toString();
        String groupOwesMoney = debtManager.getGroupOwesMoney("GroupName", "userTesting1").stream()
            .findFirst().get().toString();

        assertEquals("userTesting1 friendTesting1 15.0 reason", groupMoneyOwed,
            "Expected userTesting1 to have a debt towards friendTesting1");

        assertEquals("userTesting1 friendTesting1 15.0 reason", groupOwesMoney,
            "Expected userTesting1 to have a debt towards friendTesting1");
    }

    @Test
    void testSplitBill()
        throws UserAlreadyExistsException, ServerErrorException, NonPositiveAmountException, IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("debtor", "pass1", "name1", "family1");
        repository.registerUser("creditor", "pass1", "name1", "family1");

        debtManager.splitBill("creditor", "debtor", 20, "reason");

        String owesMoneyResponse = debtManager.getOwesMoney("debtor").stream()
            .findFirst().get().toString();
        String moneyOwedResponse = debtManager.getMoneyOwed("creditor").stream()
            .findFirst().get().toString();

        assertEquals("debtor creditor 10.0 reason", owesMoneyResponse,
            "Expected the debtor to owe the creditor 10");

        assertEquals("debtor creditor 10.0 reason", moneyOwedResponse,
            "Expected the debtor to owe the creditor 10");

        Files.deleteIfExists(Path.of("debtor_debts.txt"));
        Files.deleteIfExists(Path.of("debtor_new_debts.txt"));
    }

    @Test
    void testSplitBillWithRecalculating()
        throws UserAlreadyExistsException, ServerErrorException, NonPositiveAmountException, IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("debtorUser", "pass1", "name1", "family1");
        repository.registerUser("creditorUser", "pass1", "name1", "family1");

        debtManager.splitBill("creditorUser", "debtorUser", 20, "reason");
        debtManager.splitBill("debtorUser", "creditorUser", 30, "another reason");

        String owesMoneyResponse = debtManager.getOwesMoney("creditorUser").stream()
            .findFirst().get().toString();
        String moneyOwedResponse = debtManager.getMoneyOwed("debtorUser").stream()
            .findFirst().get().toString();

        assertEquals("creditorUser debtorUser 5.0 another reason", owesMoneyResponse,
            "Expected the creditorUser to owe the debtorUser 5");

        assertEquals("creditorUser debtorUser 5.0 another reason", moneyOwedResponse,
            "Expected the creditorUser to owe the debtorUser 5");

        Files.deleteIfExists(Path.of("creditorUser_debts.txt"));
        Files.deleteIfExists(Path.of("creditorUser_new_debts.txt"));
        Files.deleteIfExists(Path.of("debtorUser_debts.txt"));
        Files.deleteIfExists(Path.of("debtorUser_new_debts.txt"));
    }

    @Test
    void testSplitBillWithRecalculatingGroupDebts()
        throws UserAlreadyExistsException, ServerErrorException, NonPositiveAmountException,
        GroupAlreadyExistsException, IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("friend1", "pass1", "name1", "family1");
        repository.registerUser("friend2", "pass1", "name1", "family1");
        repository.registerUser("friend3", "pass1", "name1", "family1");

        groupManager.addGroup("SomeGroup", Set.of("friend1", "friend2", "friend3"));

        debtManager.splitGroupBill(GroupManager.toGroup("SomeGroup"), "friend1", 30, "reason");
        debtManager.splitBill("friend2", "friend1", 30, "another reason");

        String owesMoneyResponse = debtManager.getOwesMoney("friend1").stream()
            .findFirst().get().toString();
        String moneyOwedResponse = debtManager.getMoneyOwed("friend2").stream()
            .findFirst().get().toString();

        assertEquals("friend1 friend2 5.0 another reason", owesMoneyResponse,
            "Expected friend2 to owe friend1 5");

        assertEquals("friend1 friend2 5.0 another reason", moneyOwedResponse,
            "Expected the creditorUser to owe the debtorUser 5");

        Files.deleteIfExists(Path.of("friend1_debts.txt"));
        Files.deleteIfExists(Path.of("friend1_new_debts.txt"));
        Files.deleteIfExists(Path.of("friend2_new_group_debts.txt"));
        Files.deleteIfExists(Path.of("friend3_new_group_debts.txt"));
        Files.deleteIfExists(Path.of("SomeGroup_group_debts.txt"));
    }

    @Test
    void testPayWhenUsernameIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> debtManager.pay(null, 5, "friend", new HashSet<>()),
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testPayWhenAmountIsNonPositive() {
        assertThrows(NonPositiveAmountException.class,
            () -> debtManager.pay("user", -5, "friend", new HashSet<>()),
            "Expected IllegalArgumentException to be thrown when the amount is non positive");
    }

    @Test
    void testPayWhenUserDoNotOweAnything() throws UserAlreadyExistsException, ServerErrorException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("toBePaid", "pass", "name", "family");
        repository.registerUser("payer", "pass", "name", "family");

        assertThrows(NoDebtsToBePaidException.class,
            () -> debtManager.pay("toBePaid", 10, "payer", new HashSet<>()),
            "Expected NoDebtsToBePaidException to be thrown when the payer doesn't have any debts");
    }

    @Test
    void testPay()
        throws UserAlreadyExistsException, ServerErrorException, NonPositiveAmountException, NoDebtsToBePaidException,
        IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("paidUser", "pass", "name", "family");
        repository.registerUser("payerFriend", "pass", "name", "family");

        debtManager.splitBill("paidUser", "payerFriend", 20, "reason");

        String paidDebt = debtManager.pay("paidUser", 10, "payerFriend", new HashSet<>()).stream()
            .findFirst().get().toString();

        assertEquals("payerFriend paidUser 10.0 reason", paidDebt,
            "Expected payerFriend to have paid it's debt towards paidUser");

        Files.deleteIfExists(Path.of("payerFriend_debts.txt"));
        Files.deleteIfExists(Path.of("payerFriend_new_debts.txt"));
        Files.deleteIfExists(Path.of("payerFriend_paid_debts.txt"));
    }
}
