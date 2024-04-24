package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.IncorrectPasswordException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserNotFoundException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;

import java.nio.channels.SelectionKey;

public class LoginCommand extends Command {
    private final String username;
    private final String password;
    private final SelectionKey key;

    public LoginCommand(String username, String password, SelectionKey key) {
        this.username = username;
        this.password = password;
        this.key = key;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password cannot be null");
        }

        String notifications;
        try {
            userRepository.loginUser(username, password);
            User user = UserRepository.toUser(username);
            notifications = user.getNotifications();
        } catch (UserNotFoundException | IncorrectPasswordException e) {
            return e.getMessage();
        }

        key.attach(username);
        return String.format("User %s successfully logged in!\n", username) + notifications;
    }
}
