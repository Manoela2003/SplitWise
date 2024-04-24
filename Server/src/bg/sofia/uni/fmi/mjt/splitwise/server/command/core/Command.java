package bg.sofia.uni.fmi.mjt.splitwise.server.command.core;

import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.friends.FriendsManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;

public abstract class Command implements CommandAPI {
    protected UserRepository userRepository;
    protected GroupManager groupManager;
    protected DebtManager debtManager;
    protected FriendsManager friendsManager;

    public Command configure(UserRepository userRepository, GroupManager groupManager, DebtManager debtManager,
                             FriendsManager friendsManager) {
        this.groupManager = groupManager;
        this.userRepository = userRepository;
        this.debtManager = debtManager;
        this.friendsManager = friendsManager;
        return this;
    }

}