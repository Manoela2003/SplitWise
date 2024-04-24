package bg.sofia.uni.fmi.mjt.splitwise.server.groups;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.GroupAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroupManagerTest {
    private final GroupManager groupManager = GroupManager.getInstance();
    private static final Path GROUPS_PATH = Path.of("groups.txt");

    @AfterAll
    static void clearFiles() throws IOException {
        Files.deleteIfExists(GROUPS_PATH);
        Files.deleteIfExists(Path.of("users.txt"));
        Files.deleteIfExists(Path.of("usersPasswords.txt"));
    }

    @Test
    void testAddGroupWhenGroupNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> groupManager.addGroup(null, null),
            "Expected IllegalArgumentException to be thrown when group name is null");
    }

    @Test
    void testAddGroupWhenGroupAlreadyExists() throws GroupAlreadyExistsException, ServerErrorException {
        groupManager.addGroup("TestGroup", new HashSet<>());

        assertThrows(GroupAlreadyExistsException.class, () -> groupManager.addGroup("TestGroup", new HashSet<>()),
            "Expected GroupAlreadyExistsException to be thrown when group already exists");
    }

    @Test
    void testAddGroup()
        throws GroupAlreadyExistsException, ServerErrorException, UserAlreadyExistsException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("User1", "pass1", "test1", "test1");
        repository.registerUser("User2", "pass2", "test2", "test2");

        Set<String> members = new HashSet<>();
        members.add("User1");
        members.add("User2");

        groupManager.addGroup("groupTest", members);
        assertTrue(UserRepository.toUser("User1").isInGroup("groupTest"),
            "Expected User1 to be part of the following group: groupTest");
    }

    @Test
    void testExistsGroupWhenGroupNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> groupManager.existsGroup(null),
            "Expected IllegalArgumentException to be thrown when group name is null");
    }

    @Test
    void testExistsGroupReturnsTrue() throws GroupAlreadyExistsException, ServerErrorException {
        groupManager.addGroup("group", new HashSet<>());
        assertTrue(groupManager.existsGroup("group"),
            "Expected existsGroup to return true when the group exists");
    }

    @Test
    void testExistsGroupReturnsFalse() {
        assertFalse(groupManager.existsGroup("test"),
            "Expected existsGroup to return false when the group doesn't exists");
    }

    @Test
    void testToGroupWhenGroupNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> GroupManager.toGroup(null),
            "Expected IllegalArgumentException to be thrown when group name is null");
    }

    @Test
    void testToGroupWhenGroupDoNotExist() {
        assertThrows(ServerErrorException.class, () -> GroupManager.toGroup("test"),
            "Expected ServerErrorException to be thrown when group doesn't exist");
    }

    @Test
    void testToGroup() throws GroupAlreadyExistsException, ServerErrorException {
        groupManager.addGroup("GroupTesting", new HashSet<>());

        assertEquals("GroupTesting", GroupManager.toGroup("GroupTesting").name(),
            "Expected the returned group to be the one with name: GroupTesting");
    }

    @Test
    void testInitialize() throws ServerErrorException, UserAlreadyExistsException, IOException {
        UserRepository repository = UserRepository.getInstance();
        repository.registerUser("testUser1", "p", "p", "p");
        repository.registerUser("testUser2", "p", "p", "p");
        repository.registerUser("testUser3", "p", "p", "p");

        Files.deleteIfExists(GROUPS_PATH);

        Files.writeString(GROUPS_PATH, "GroupTest:[testUser1, testUser2, testUser3]:");

        groupManager.initialize();

        assertTrue(GroupManager.getGroups().contains("GroupTest"),
            "Expected group: GroupTest to be loaded when initializing");
    }
}
