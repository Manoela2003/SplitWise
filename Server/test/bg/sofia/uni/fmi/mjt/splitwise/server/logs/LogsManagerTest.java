package bg.sofia.uni.fmi.mjt.splitwise.server.logs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogsManagerTest {
    private final LogsManager logsManager = LogsManager.getInstance();

    @AfterEach
    void clearFile() throws IOException {
        Files.deleteIfExists(Path.of("logs_file.txt"));
    }

    @Test
    void testAddLogToFileWhenErrorIsNull() {
        assertThrows(IllegalArgumentException.class, () -> logsManager.addLogToFile(null, "user"),
            "Expected IllegalArgumentException to be thrown when error is null");
    }

    @Test
    void testAddLogToFile() throws IOException {
        Path file = Path.of("logs_file.txt");
        Files.deleteIfExists(file);

        Throwable error = mock(Throwable.class);
        when(error.getMessage()).thenReturn("Test error message");
        when(error.getStackTrace()).thenReturn(new StackTraceElement[]
            {new StackTraceElement("TestClass", "testMethod", "TestClass.java", 10)});

        logsManager.addLogToFile(error, "User");
        assertTrue(Files.exists(file), "Expected the logs file to be created but it wasn't");

        String expectedLogContent = "- - - - - - - - - - - - -" + System.lineSeparator() +
            "Error occurred when User was interacting with the server" + System.lineSeparator() +
            "Error message: Test error message" + System.lineSeparator() +
            "Stack traces:" + System.lineSeparator() +
            "TestClass.testMethod(TestClass.java:10)" + System.lineSeparator();

        String actualLogContent = Files.readString(file);

        assertEquals(expectedLogContent, actualLogContent,
            "Expected error to be written to the file");
    }

}
