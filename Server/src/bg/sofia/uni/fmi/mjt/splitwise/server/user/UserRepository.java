package bg.sofia.uni.fmi.mjt.splitwise.server.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.IncorrectPasswordException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserNotFoundException;
import bg.sofia.uni.fmi.mjt.splitwise.server.passwords.PasswordsDatabase;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserRepository {
    private static final Path USERS_PATH = Path.of("users.txt");
    private static UserRepository instance;
    private static Map<String, User> users = new HashMap<>();
    private static PasswordsDatabase database = PasswordsDatabase.getInstance();

    private UserRepository() {

    }

    public void initialize() throws ServerErrorException {
        database.initialize();
        loadUsersFromFile();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }

        return instance;
    }

    static UserRepository setInstance(PasswordsDatabase db) {
        database = db;
        return getInstance();
    }

    private void loadUsersFromFile() throws ServerErrorException {
        if (!Files.exists(USERS_PATH)) {
            return;
        }

        try (var bufferedReader = Files.newBufferedReader(USERS_PATH)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] user = line.split(FileUtils.SINGLE_SPACE);

                String username = user[FileUtils.ZERO_INDEX];
                String firstName = user[FileUtils.ONE_INDEX];
                String lastName = user[FileUtils.TWO_INDEX];

                users.put(username, new User(username, firstName, lastName));
            }
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + USERS_PATH, e);
        }
    }

    public static User toUser(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        return users.get(username);
    }

    public void registerUser(String username, String password, String firstName, String lastName)
        throws UserAlreadyExistsException, ServerErrorException {
        if (username == null || password == null || firstName == null || lastName == null) {
            throw new IllegalArgumentException("Username, password and names cannot be null");
        }

        database.addUserCredentials(username, password);
        FileUtils.writeToFile(USERS_PATH, FileUtils.SINGLE_SPACE, username, firstName, lastName);
        users.put(username, new User(username, firstName, lastName));
    }

    public void loginUser(String username, String password)
        throws UserNotFoundException, IncorrectPasswordException, ServerErrorException {
        database.checkLoginCredentials(username, password);
    }

    public boolean containsUser(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        return users.containsKey(username);
    }

    public static Set<String> getUsernames() {
        return users.keySet();
    }
}
