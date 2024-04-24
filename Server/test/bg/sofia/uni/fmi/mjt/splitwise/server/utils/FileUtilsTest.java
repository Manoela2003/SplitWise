package bg.sofia.uni.fmi.mjt.splitwise.server.utils;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest {
    private static final Path file = Path.of("test.txt");

    @Test
    void testCreateFileIfNeededWhenFileNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> FileUtils.createFileIfNeeded(null),
            "Expected IllegalArgumentException when the file name is null");
    }

    @Test
    void testCreateFile() throws ServerErrorException, IOException {
        FileUtils.createFileIfNeeded(file);

        assertTrue(Files.exists(file), "Expected the file to be created but it was not");
        Files.deleteIfExists(file);
    }

    @Test
    void testWriteToFileWhenFileNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> FileUtils.writeToFile(null, "", ""),
            "Expected IllegalArgumentException when the file name is null");
    }

    @Test
    void testDeleteFileWhenFileNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> FileUtils.deleteFile(null),
            "Expected IllegalArgumentException when the file name is null");
    }

    @Test
    void testDeleteFile() throws ServerErrorException {
        FileUtils.createFileIfNeeded(file);
        FileUtils.deleteFile(file);

        assertFalse(Files.exists(file), "Expected the file to be deleted");
    }
}
