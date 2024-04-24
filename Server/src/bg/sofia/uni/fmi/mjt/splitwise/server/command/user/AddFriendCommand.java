package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.FriendAlreadyAddedException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;

public class AddFriendCommand extends Command {
    private final String username;
    private final String friendUsername;

    public AddFriendCommand(String username, String friendUsername) {
        this.username = username;
        this.friendUsername = friendUsername;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (username == null || friendUsername == null) {
            throw new IllegalArgumentException("Username and friend username cannot be null");
        }

        if (friendUsername.equals(username)) {
            return "You cannot add yourself as friend";
        }

        if (!userRepository.containsUser(friendUsername)) {
            return String.format("User %s doesn't exist", friendUsername);
        }

        try {
            friendsManager.addFriend(username, UserRepository.toUser(friendUsername));
        } catch (FriendAlreadyAddedException e) {
            return e.getMessage();
        }

        return String.format("User %s is successfully added to your friend list", friendUsername);
    }
}
