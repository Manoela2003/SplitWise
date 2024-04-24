package bg.sofia.uni.fmi.mjt.splitwise.server.groups;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.GroupAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManager {
    private static final Path GROUPS_PATH = Path.of("groups.txt");
    private static final String COMMA_AND_SPACE = ", ";
    private static final String EMPTY_STRING = "";
    private static final String SQUARE_BRACKETS_REMOVER_REGEX = "\\[|\\]";
    private static GroupManager instance;
    private static Map<String, Group> groups;

    private GroupManager() {
        groups = new HashMap<>();
    }

    public static GroupManager getInstance() {
        if (instance == null) {
            instance = new GroupManager();
        }
        return instance;
    }

    public void initialize() throws ServerErrorException {
        loadGroupsFromFile();
    }

    private void loadGroupsFromFile() throws ServerErrorException {
        if (!Files.exists(GROUPS_PATH)) {
            return;
        }

        try (var bufferedReader = Files.newBufferedReader(GROUPS_PATH)) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] group = line.split(FileUtils.COLON);

                String groupName = group[FileUtils.ZERO_INDEX];
                String usernames = group[FileUtils.ONE_INDEX].replaceAll(SQUARE_BRACKETS_REMOVER_REGEX, EMPTY_STRING);
                Set<String> users = new HashSet<>(Arrays.asList(usernames.split(COMMA_AND_SPACE)));

                Set<User> members = users.stream()
                    .map(UserRepository::toUser)
                    .collect(Collectors.toSet());

                members.forEach(user -> user.addToGroup(groupName));

                groups.put(groupName, new Group(groupName, members));
            }
        } catch (IOException e) {
            throw new ServerErrorException(FileUtils.READING_FROM_FILE_ERROR + GROUPS_PATH, e);
        }
    }

    public void addGroup(String groupName, Set<String> members) throws GroupAlreadyExistsException,
        ServerErrorException {
        if (groupName == null || members == null) {
            throw new IllegalArgumentException("Group name and members cannot be null");
        }

        Set<User> users = members.stream()
            .distinct()
            .map(UserRepository::toUser)
            .collect(Collectors.toSet());

        if (groups.containsKey(groupName)) {
            throw new GroupAlreadyExistsException(String.format("The group %s already exists", groupName));
        }

        groups.put(groupName, new Group(groupName, users));
        users.forEach(user -> user.addToGroup(groupName));

        FileUtils.writeToFile(GROUPS_PATH, FileUtils.COLON, groupName, members);
    }

    public boolean existsGroup(String groupName) {
        if (groupName == null) {
            throw new IllegalArgumentException("Group name cannot be null");
        }

        return groups.containsKey(groupName);
    }

    public static Group toGroup(String groupName) throws ServerErrorException {
        if (groupName == null) {
            throw new IllegalArgumentException("Group name cannot be null");
        }

        if (!groups.containsKey(groupName)) {
            throw new ServerErrorException("Invalid map of groups");
        }

        return groups.get(groupName);
    }

    public static Set<String> getGroups() {
        return groups.keySet();
    }
}
