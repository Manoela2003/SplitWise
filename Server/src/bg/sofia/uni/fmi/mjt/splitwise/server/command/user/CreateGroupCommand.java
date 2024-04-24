package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.GroupAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;

import java.util.Set;

public class CreateGroupCommand extends Command {
    private final String groupName;
    private final Set<String> members;
    private final String username;

    public CreateGroupCommand(String groupName, Set<String> members, String username) {
        this.groupName = groupName;
        this.members = members;
        this.username = username;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (groupName == null || members == null || username == null) {
            throw new IllegalArgumentException("Group name, username and group members cannot be null");
        }

        if (members.size() <= 1) {
            return "Group members cannot be less than 2";
        }

        if (members.contains(username)) {
            return "Failed to create group. Don't explicitly add your username";
        }

        for (String member : members) {
            if (!userRepository.containsUser(member)) {
                return "Failed to create group. Invalid group member username: " + member;
            }
        }

        try {
            members.add(username);
            groupManager.addGroup(groupName, members);
        } catch (GroupAlreadyExistsException e) {
            return e.getMessage();
        }
        return String.format("Group %s is successfully created", groupName);
    }
}