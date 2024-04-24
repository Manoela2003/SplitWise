package bg.sofia.uni.fmi.mjt.splitwise.server.logs;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LogsManager {
    private static final Path LOGS_PATH = Path.of("logs_file.txt");
    private static final String DIVIDER = "- - - - - - - - - - - - -" + System.lineSeparator();
    private static LogsManager instance;

    private LogsManager() {

    }

    public static LogsManager getInstance() {
        if (instance == null) {
            instance = new LogsManager();
        }
        return instance;
    }

    public void addLogToFile(Throwable error, String username) {
        if (error == null) {
            throw new IllegalArgumentException("Error cannot be null");
        }

        try {
            FileUtils.createFileIfNeeded(LOGS_PATH);
        } catch (ServerErrorException e) {
            throw new IllegalStateException(FileUtils.WRITING_TO_FILE_ERROR + LOGS_PATH, e);
        }

        try (var bufferedWriter = Files.newBufferedWriter(LOGS_PATH, StandardOpenOption.APPEND)) {
            writeToFile(bufferedWriter, error, username);
        } catch (IOException e) {
            throw new IllegalStateException(FileUtils.WRITING_TO_FILE_ERROR + LOGS_PATH, e);
        }
    }

    private void writeToFile(BufferedWriter bufferedWriter, Throwable error, String username) throws IOException {
        bufferedWriter.write(DIVIDER);

        if (username != null) {
            bufferedWriter.write(String.format("Error occurred when %s was interacting with the server", username) +
                System.lineSeparator());
        }

        bufferedWriter.write("Error message: " + error.getMessage() + System.lineSeparator());
        bufferedWriter.write("Stack traces:" + System.lineSeparator());

        for (StackTraceElement element : error.getStackTrace()) {
            bufferedWriter.write(element.toString() + System.lineSeparator());
        }

        bufferedWriter.flush();
    }
}
