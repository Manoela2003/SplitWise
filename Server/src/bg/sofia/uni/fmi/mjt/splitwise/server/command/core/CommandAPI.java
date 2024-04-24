package bg.sofia.uni.fmi.mjt.splitwise.server.command.core;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;

public interface CommandAPI {
    String execute() throws ServerErrorException;
}
