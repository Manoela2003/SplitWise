package bg.sofia.uni.fmi.mjt.splitwise.server.utils;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

public class FileUtils {
    private static final String ERROR_MESSAGE = "A problem occurred while creating the following file: ";
    public static final String READING_FROM_FILE_ERROR = "A problem occurred while reading from the following file: ";
    public static final String WRITING_TO_FILE_ERROR = "A problem occurred while writing to the following file: ";
    public static final String DELETING_FILE_ERROR = "A problem occurred while trying to delete the following file: ";
    public static final String SINGLE_SPACE = " ";
    public static final String COLON = ":";
    public static final int ZERO_INDEX = 0;
    public static final int ONE_INDEX = 1;
    public static final int TWO_INDEX = 2;
    public static final int THREE_INDEX = 3;
    public static final int FOUR_INDEX = 4;

    public static void createFileIfNeeded(Path fileName) throws ServerErrorException {
        if (fileName == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(fileName)) {
            try {
                Files.createFile(fileName);
            } catch (IOException e) {
                throw new ServerErrorException(ERROR_MESSAGE + fileName, e);
            }
        }
    }

    public static void writeToFile(Path fileName, String delimiter, Object... values) throws ServerErrorException {
        if (fileName == null || Arrays.stream(values)
            .anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("File name and values cannot be null");
        }

        createFileIfNeeded(fileName);

        try (var bufferedWriter = Files.newBufferedWriter(fileName, StandardOpenOption.APPEND)) {

            for (Object value : values) {
                bufferedWriter.write(value + delimiter);
            }

            if (!delimiter.equals(System.lineSeparator())) {
                bufferedWriter.write(System.lineSeparator());
            }

            bufferedWriter.flush();

        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.WRITING_TO_FILE_ERROR + fileName, e);
        }
    }

    public static void deleteFile(Path fileName) throws ServerErrorException {
        if (fileName == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        try {
            Files.deleteIfExists(fileName);
        } catch (IOException e) {
            throw new ServerErrorException(DELETING_FILE_ERROR + fileName, e);
        }
    }
}
