package bg.sofia.uni.fmi.mjt.splitwise.server.command.core;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.AddFriendCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.CreateGroupCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.GetStatusCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.LoginCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.PayCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.RegisterCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.SeeTransactionsCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.SplitCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.SplitGroupCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import org.junit.jupiter.api.Test;

import java.nio.channels.SelectionKey;

import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.FOUR_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.ONE_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.THREE_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.TWO_INDEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandCreatorTest {
    private static final String INVALID_ARGS_COUNT_MESSAGE_FORMAT =
        "Invalid count of arguments: \"%s\" expects %s arguments. Example: \"%s\"";
    private final SelectionKey key = mock(SelectionKey.class);

    @Test
    void testCreateWhenClientInputIsNull() {
        assertThrows(IllegalArgumentException.class, () -> CommandCreator.create(null, null),
            "Expected IllegalArgumentException to be thrown when client input is null");
    }

    @Test
    void testCreateRegisterWhenUserIsAlreadyLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals("You are already logged in!",
            CommandCreator.create("register user pass firstName familyName", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateRegisterWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "register", FOUR_INDEX,
            "register" + " <username> <password> <first name> <last name>"),
            CommandCreator.create("register user pass firstName", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateRegisterSuccess() {
        when(key.attachment()).thenReturn(null);

        assertTrue(CommandCreator.create("register user pass firstName familyName", key) instanceof RegisterCommand,
            "Expected a RegisterCommand to be returned");
    }

    @Test
    void testCreateLoginWhenUserIsAlreadyLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals("You are already logged in!",
            CommandCreator.create("login user pass", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateLoginWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "login", TWO_INDEX,
                "login" + " <username> <password>"),
            CommandCreator.create("login user pass firstName", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateLoginSuccess() {
        when(key.attachment()).thenReturn(null);

        assertTrue(CommandCreator.create("login user pass", key) instanceof LoginCommand,
            "Expected a LoginCommand to be returned");
    }

    @Test
    void testCreateAddFriendWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("add-friend user", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testCreateAddFriendWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "add-friend", ONE_INDEX,
                "add-friend" + " <username>"),
            CommandCreator.create("add-friend user friend", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateAddFriendSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("add-friend friend", key) instanceof AddFriendCommand,
            "Expected an AddFriendCommand to be returned");
    }

    @Test
    void testCreateGroupWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("create-group Name friend1 friend2", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testCreateGroupWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "create-group", "3 or more",
                "create-group" + " <group_name> <username> <username> ... <username>"),
            CommandCreator.create("create-group Name friend1", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateGroupWhenUserIsIncluded() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals("Failed to create group. Don't include your own username",
            CommandCreator.create("create-group Name friend1 user", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateGroupSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("create-group Name friend1 friend2", key) instanceof CreateGroupCommand,
            "Expected a CreateGroupCommand to be returned");
    }

    @Test
    void testCreateSplitWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("split 10 friend reason", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testCreateSplitWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "split", THREE_INDEX,
                "split" + " <amount> <username> <reason_for_payment>"),
            CommandCreator.create("split 10 friend", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateSplitSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("split 10 friend reason", key) instanceof SplitCommand,
            "Expected a SplitCommand to be returned");
    }

    @Test
    void testCreateSplitGroupWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("split-group 10 GroupName reason", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testCreateSplitGroupWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "split-group", THREE_INDEX,
                "split-group" + " <amount> <group_name> <reason_for_payment>"),
            CommandCreator.create("split-group 10 GroupName", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testCreateSplitGroupSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("split-group 10 GroupName reason", key) instanceof SplitGroupCommand,
            "Expected a SplitGroupCommand to be returned");
    }

    @Test
    void testGetStatusWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("get-status", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testGetStatusWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals("get-status command shouldn't have any arguments",
            CommandCreator.create("get-status user", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testGetStatusSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("get-status", key) instanceof GetStatusCommand,
            "Expected a GetStatusCommand to be returned");
    }

    @Test
    void testPaidWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("paid 10 user", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testPaidWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, "paid", TWO_INDEX,
                "paid" + " <amount> <username>"),
            CommandCreator.create("paid 10 user test", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testPaidSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("paid 10 user", key) instanceof PayCommand,
            "Expected a PayCommand to be returned");
    }

    @Test
    void testSeeTransactionsWhenUserIsNotLogged() throws ServerErrorException {
        when(key.attachment()).thenReturn(null);

        assertEquals("You must register or log in first!",
            CommandCreator.create("see-transactions", key).execute(),
            "Expected a MissingPermissionCommand to be returned");
    }

    @Test
    void testSeeTransactionsWhenArgsLengthIsIncorrect() throws ServerErrorException {
        when(key.attachment()).thenReturn("user");

        assertEquals("see-transactions command shouldn't have any arguments",
            CommandCreator.create("see-transactions user", key).execute(),
            "Expected an InvalidCommand to be returned");
    }

    @Test
    void testSeeTransactionsSuccess() {
        when(key.attachment()).thenReturn("user");

        assertTrue(CommandCreator.create("see-transactions", key) instanceof SeeTransactionsCommand,
            "Expected a SeeTransactionsCommand to be returned");
    }
}
