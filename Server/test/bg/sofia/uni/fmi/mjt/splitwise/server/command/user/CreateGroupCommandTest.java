package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.GroupAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateGroupCommandTest {

    @Test
    void testExecuteWhenUsernameIsNull() {
        Command command = new CreateGroupCommand("group", new HashSet<>(), null);

        assertThrows(IllegalArgumentException.class, command::execute,
            "Expected IllegalArgumentException to be thrown when username is null");
    }

    @Test
    void testExecuteWhenGroupMembersAreInsufficient() throws ServerErrorException {
        Set<String> members = new HashSet<>();
        members.add("member1");

        Command command = new CreateGroupCommand("group", members, "user");

        assertEquals("Group members cannot be less than 2", command.execute(),
            "Expected a message: \"Group members cannot be less than 2\" to be shown but it was not");
    }

    @Test
    void testExecuteWhenUserAddsHimselfToGroup() throws ServerErrorException {
        Set<String> members = new HashSet<>();
        members.add("member1");
        members.add("member2");

        Command command = new CreateGroupCommand("group", members, "member2");

        assertEquals("Failed to create group. Don't explicitly add your username", command.execute(),
            "Expected a message: \"Failed to create group. Don't explicitly add your username\"" +
                " to be shown but it was not");
    }

    @Test
    void testExecuteWhenMemberNameIsInvalid() throws ServerErrorException {
        UserRepository userRepository = mock(UserRepository.class);
        Set<String> members = new HashSet<>();
        members.add("member1");
        members.add("member2");

        Command command = new CreateGroupCommand("group", members, "user")
            .configure(userRepository, null, null, null);

        assertEquals("Failed to create group. Invalid group member username: member2", command.execute(),
            "Expected a message: \"Failed to create group. Invalid group member username: member2\"" +
                " to be shown but it was not");
    }

    @Test
    void testExecuteWhenAddGroupThrows() throws GroupAlreadyExistsException, ServerErrorException {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.containsUser("member1")).thenReturn(true);
        when(userRepository.containsUser("member2")).thenReturn(true);
        GroupManager manager = mock(GroupManager.class);
        Set<String> members = new HashSet<>();
        members.add("member1");
        members.add("member2");

        Set<String> allMembers = new HashSet<>(Set.copyOf(members));
        allMembers.add("user");

        Command command = new CreateGroupCommand("group", members, "user")
            .configure(userRepository, manager, null, null);

        doThrow(new GroupAlreadyExistsException("The group group already exists"))
            .when(manager)
            .addGroup("group", allMembers);

        assertEquals("The group group already exists", command.execute(),
            "Expected a message: \"The group group already exists\" to be shown but it was not");
    }

    @Test
    void testExecute() throws ServerErrorException, GroupAlreadyExistsException {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.containsUser("member1")).thenReturn(true);
        when(userRepository.containsUser("member2")).thenReturn(true);
        GroupManager manager = mock(GroupManager.class);
        Set<String> members = new HashSet<>();
        members.add("member1");
        members.add("member2");

        Command command = new CreateGroupCommand("group", members, "user")
            .configure(userRepository, manager, null, null);

        assertEquals("Group group is successfully created", command.execute(),
            "Expected a message: \"Group group is successfully created\" to be shown but it was not");

        members.add("user");
        verify(manager, times(1)).addGroup("group", members);
    }
}
