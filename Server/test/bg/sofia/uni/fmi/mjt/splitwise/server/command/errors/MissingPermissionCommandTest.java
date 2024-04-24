package bg.sofia.uni.fmi.mjt.splitwise.server.command.errors;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MissingPermissionCommandTest {
    @Test
    void testExecute() throws ServerErrorException {
        Command command = new MissingPermissionCommand("Error message");

        assertEquals("Error message", command.execute(),
            "Expected the following message to be returned: Error message");
    }
}
