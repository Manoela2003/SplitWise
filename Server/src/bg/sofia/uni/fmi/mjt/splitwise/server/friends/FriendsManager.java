package bg.sofia.uni.fmi.mjt.splitwise.server.friends;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.FriendAlreadyAddedException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FriendsManager {
    private static final Path USERS_FRIENDS_PATH = Path.of("usersFriends.txt");
    private final Map<String, Set<User>> friends = new HashMap<>();
    private static FriendsManager instance;

    private FriendsManager() {

    }

    public static FriendsManager getInstance() {
        if (instance == null) {
            instance = new FriendsManager();
        }
        return instance;
    }

    public void initialize() throws ServerErrorException {
        loadFriendsFromFile();
    }

    public void loadFriendsFromFile() throws ServerErrorException {
        if (!Files.exists(USERS_FRIENDS_PATH)) {
            return;
        }

        try (var bufferedReader = Files.newBufferedReader(USERS_FRIENDS_PATH)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] friendship = line.split(FileUtils.SINGLE_SPACE);

                String username = friendship[FileUtils.ZERO_INDEX];
                User friend = UserRepository.toUser(friendship[FileUtils.ONE_INDEX]);

                if (!friends.containsKey(username)) {
                    friends.put(username, new HashSet<>());
                }
                friends.get(username).add(friend);
            }
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + USERS_FRIENDS_PATH, e);
        }
    }

    public void addFriend(String username, User friend) throws FriendAlreadyAddedException, ServerErrorException {
        if (username == null || friend == null ) {
            throw new IllegalArgumentException("Friend and username cannot be null");
        }

        if (friends.containsKey(username) && friends.get(username).contains(friend)) {
            throw new FriendAlreadyAddedException(
                String.format("User %s is already in your friend list", friend.getUsername()));
        }

        if (!friends.containsKey(username)) {
            friends.put(username, new HashSet<>());
        }
        friends.get(username).add(friend);

        FileUtils.writeToFile(USERS_FRIENDS_PATH, FileUtils.SINGLE_SPACE, username, friend.getUsername());
    }

    public boolean hasFriend(String username, User friend) {
        if (username == null || friend == null) {
            throw new IllegalArgumentException("Username and friend cannot be null");
        }

        return friends.containsKey(username) && friends.get(username).contains(friend);
    }
}
