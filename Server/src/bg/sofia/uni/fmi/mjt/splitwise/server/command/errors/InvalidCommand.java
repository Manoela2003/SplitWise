package bg.sofia.uni.fmi.mjt.splitwise.server.command.errors;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;

public class InvalidCommand extends Command {
    private final String errorMessage;

    public InvalidCommand(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String execute() {
        return errorMessage;
    }
}
