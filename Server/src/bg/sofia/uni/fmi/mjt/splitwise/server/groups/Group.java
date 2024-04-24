package bg.sofia.uni.fmi.mjt.splitwise.server.groups;

import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;

import java.util.Set;

public record Group(String name, Set<User> members) {
}
