package bg.sofia.uni.fmi.mjt.splitwise.server.passwords;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.IncorrectPasswordException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserNotFoundException;
import bg.sofia.uni.fmi.mjt.splitwise.server.passwords.encryption.PasswordEncryption;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils.writeToFile;

public class PasswordsDatabase {
    private static final Path USERS_PASSWORDS_PATH = Path.of("usersPasswords.txt");
    private final Map<String, Password> usersPasswords = new HashMap<>();
    private static PasswordsDatabase instance;

    private PasswordsDatabase() {

    }

    public static PasswordsDatabase getInstance() {
        if (instance == null) {
            instance = new PasswordsDatabase();
        }
        return instance;
    }

    public void initialize() throws ServerErrorException {
        loadUsersPasswords();
    }

    private void loadUsersPasswords() throws ServerErrorException {
        if (!Files.exists(USERS_PASSWORDS_PATH)) {
            return;
        }

        try (var bufferedReader = Files.newBufferedReader(USERS_PASSWORDS_PATH)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] credentials = line.split(FileUtils.SINGLE_SPACE);

                String username = credentials[FileUtils.ZERO_INDEX];
                String hashedPass = credentials[FileUtils.ONE_INDEX];
                byte[] salt = Base64.getDecoder().decode(credentials[FileUtils.TWO_INDEX]);

                usersPasswords.put(username, new Password(hashedPass, salt));
            }
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + USERS_PASSWORDS_PATH, e);
        }
    }

    public void addUserCredentials(String username, String password)
        throws UserAlreadyExistsException, ServerErrorException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username or password cannot be null");
        }

        if (usersPasswords.containsKey(username)) {
            throw new UserAlreadyExistsException("The username already exists");
        }

        byte[] salt = PasswordEncryption.getSalt();
        String hashedPass = PasswordEncryption.hashPassword(password, salt);

        usersPasswords.put(username, new Password(hashedPass, salt));
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        writeToFile(USERS_PASSWORDS_PATH, FileUtils.SINGLE_SPACE, username, hashedPass, saltBase64);
    }

    public void checkLoginCredentials(String username, String password)
        throws UserNotFoundException, IncorrectPasswordException, ServerErrorException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username or password cannot be null");
        }

        if (!usersPasswords.containsKey(username)) {
            throw new UserNotFoundException("Incorrect username");
        } else if (!passwordMatchesHash(username, password)) {
            throw new IncorrectPasswordException("Incorrect password");
        }
    }

    private boolean passwordMatchesHash(String username, String password) throws ServerErrorException {
        byte[] salt = usersPasswords.get(username).salt();
        String givenPassword = PasswordEncryption.hashPassword(password, salt);

        return usersPasswords.get(username).hash().equals(givenPassword);
    }

    public Map<String, Password> getUsersPasswords() {
        return usersPasswords;
    }
}
