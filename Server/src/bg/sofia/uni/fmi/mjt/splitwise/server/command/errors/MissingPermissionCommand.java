package bg.sofia.uni.fmi.mjt.splitwise.server.command.errors;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;

public class MissingPermissionCommand extends Command {
    private final String commandError;

    public MissingPermissionCommand(String error) {
        this.commandError = error;
    }

    @Override
    public String execute() {
        return commandError;
    }
}
