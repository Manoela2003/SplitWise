package bg.sofia.uni.fmi.mjt.splitwise.server.command.core;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.AddFriendCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.CreateGroupCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.GetStatusCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.errors.InvalidCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.LoginCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.errors.MissingPermissionCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.PayCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.RegisterCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.SeeTransactionsCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.SplitCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.command.user.SplitGroupCommand;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.FOUR_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.ONE_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.THREE_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.TWO_INDEX;
import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.ZERO_INDEX;

public class CommandCreator {
    private static final String REGISTER = "register";
    private static final String LOGIN = "login";
    private static final String ADD_FRIEND = "add-friend";
    private static final String CREATE_GROUP = "create-group";
    private static final String SPLIT = "split";
    private static final String SPLIT_GROUP = "split-group";
    private static final String GET_STATUS = "get-status";
    private static final String PAID = "paid";
    private static final String SEE_TRANSACTIONS = "see-transactions";
    private static final String INVALID_ARGS_COUNT_MESSAGE_FORMAT =
        "Invalid count of arguments: \"%s\" expects %s arguments. Example: \"%s\"";

    private static List<String> getCommandArguments(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        boolean insideQuote = false;

        for (char c : input.toCharArray()) {
            if (c == '"') {
                insideQuote = !insideQuote;
            }
            if (c == ' ' && !insideQuote) {
                tokens.add(sb.toString().replace("\"", ""));
                sb.delete(0, sb.length());
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().replace("\"", ""));

        return tokens;
    }

    public static Command create(String clientInput, SelectionKey key) {
        if (clientInput == null) {
            throw new IllegalArgumentException("Client input cannot be null");
        }

        List<String> tokens = CommandCreator.getCommandArguments(clientInput);
        String[] args = tokens.subList(ONE_INDEX, tokens.size()).toArray(new String[ZERO_INDEX]);

        return switch (tokens.get(ZERO_INDEX)) {
            case REGISTER -> register(args, key);
            case LOGIN -> login(args, key);
            case ADD_FRIEND -> addFriend(args, (String) key.attachment());
            case CREATE_GROUP -> createGroup(args, (String) key.attachment());
            case SPLIT -> split(args, (String) key.attachment());
            case SPLIT_GROUP -> splitGroup(args, (String) key.attachment());
            case GET_STATUS -> getStatus(args, (String) key.attachment());
            case PAID -> paid(args, (String) key.attachment());
            case SEE_TRANSACTIONS -> seeTransactions(args, (String) key.attachment());
            default -> new InvalidCommand("Unknown command");
        };
    }

    private static Command register(String[] args, SelectionKey key) {
        if (key.attachment() != null) {
            return new InvalidCommand("You are already logged in!");
        }

        if (args.length != FOUR_INDEX) {
            return new InvalidCommand(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, REGISTER, FOUR_INDEX,
                REGISTER + " <username> <password> <first name> <last name>"));
        }

        return new RegisterCommand(args[ZERO_INDEX], args[ONE_INDEX], args[TWO_INDEX], args[THREE_INDEX], key);
    }

    private static Command login(String[] args, SelectionKey key) {
        if (key.attachment() != null) {
            return new InvalidCommand("You are already logged in!");
        }

        if (args.length != TWO_INDEX) {
            return new InvalidCommand(
                String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, LOGIN, TWO_INDEX, LOGIN + " <username> <password>"));
        }

        return new LoginCommand(args[ZERO_INDEX], args[ONE_INDEX], key);
    }

    private static Command addFriend(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length != ONE_INDEX) {
            return new InvalidCommand(
                String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, ADD_FRIEND, ONE_INDEX, ADD_FRIEND + " <username>"));
        }

        return new AddFriendCommand(username, args[ZERO_INDEX]);
    }

    private static Command createGroup(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length <= TWO_INDEX) {
            return new InvalidCommand(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, CREATE_GROUP, "3 or more",
                CREATE_GROUP + " <group_name> <username> <username> ... <username>"));
        }

        if (Arrays.asList(args).contains(username)) {
            return new InvalidCommand("Failed to create group. Don't include your own username");
        }

        Set<String> members = Arrays.stream(args)
            .skip(ONE_INDEX)
            .collect(Collectors.toSet());

        return new CreateGroupCommand(args[ZERO_INDEX], members, username);
    }

    private static Command split(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length <= TWO_INDEX) {
            return new InvalidCommand(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, SPLIT, THREE_INDEX,
                SPLIT + " <amount> <username> <reason_for_payment>"));
        }

        String reason = Arrays.stream(args)
            .skip(TWO_INDEX)
            .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

        return new SplitCommand(username, args[ONE_INDEX], args[ZERO_INDEX], reason);
    }

    private static Command splitGroup(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length < THREE_INDEX) {
            return new InvalidCommand(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, SPLIT_GROUP, THREE_INDEX,
                SPLIT_GROUP + " <amount> <group_name> <reason_for_payment>"));
        }

        String reason = Arrays.stream(args)
            .skip(TWO_INDEX)
            .collect(Collectors.joining(FileUtils.SINGLE_SPACE));

        return new SplitGroupCommand(username, args[ONE_INDEX], args[ZERO_INDEX], reason);
    }

    private static Command getStatus(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length != ZERO_INDEX) {
            return new InvalidCommand("get-status command shouldn't have any arguments");
        }

        return new GetStatusCommand(username);
    }

    private static Command paid(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length != TWO_INDEX) {
            return new InvalidCommand(String.format(INVALID_ARGS_COUNT_MESSAGE_FORMAT, PAID, TWO_INDEX,
                PAID + " <amount> <username>"));
        }

        return new PayCommand(username, args[ONE_INDEX], args[ZERO_INDEX]);
    }

    private static Command seeTransactions(String[] args, String username) {
        if (username == null) {
            return new MissingPermissionCommand("You must register or log in first!");
        }

        if (args.length != ZERO_INDEX) {
            return new InvalidCommand("see-transactions command shouldn't have any arguments");
        }

        return new SeeTransactionsCommand(username);
    }
}
