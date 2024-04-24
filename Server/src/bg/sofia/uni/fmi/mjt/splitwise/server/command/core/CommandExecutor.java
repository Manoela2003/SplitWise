package bg.sofia.uni.fmi.mjt.splitwise.server.command.core;

import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.friends.FriendsManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;

import java.nio.channels.SelectionKey;

public class CommandExecutor {
    private final UserRepository userRepository;
    private final GroupManager groupManager;
    private final DebtManager debtManager;
    private final FriendsManager friendsManager;

    public static CommandExecutor configure(UserRepository userRepository, GroupManager groupManager,
                                            DebtManager debtManager, FriendsManager friendsManager) {
        return new CommandExecutor(userRepository, groupManager, debtManager, friendsManager);
    }

    private CommandExecutor(UserRepository userRepository, GroupManager groupManager, DebtManager debtManager,
                            FriendsManager friendsManager) {
        this.userRepository = userRepository;
        this.groupManager = groupManager;
        this.debtManager = debtManager;
        this.friendsManager = friendsManager;
    }

    public String execute(String cmd, SelectionKey key) throws ServerErrorException {
        return CommandCreator.create(cmd, key).configure(userRepository, groupManager, debtManager, friendsManager)
            .execute();
    }
}
