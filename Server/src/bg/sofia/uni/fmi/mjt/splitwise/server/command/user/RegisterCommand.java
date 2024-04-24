package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;

import java.nio.channels.SelectionKey;

public class RegisterCommand extends Command {
    private final String username;
    private final String password;
    private final String firstName;
    private final String familyName;
    private final SelectionKey key;

    public RegisterCommand(String username, String password, String firstName, String familyName, SelectionKey key) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.familyName = familyName;
        this.key = key;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (username == null || password == null || firstName == null || familyName == null) {
            throw new IllegalArgumentException("Username, password and names cannot be null");
        }

        try {
            userRepository.registerUser(username, password, firstName, familyName);
        } catch (UserAlreadyExistsException e) {
            return e.getMessage();
        }

        key.attach(username);
        return String.format("User %s successfully registered!", username);
    }
}
