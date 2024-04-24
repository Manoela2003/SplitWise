package bg.sofia.uni.fmi.mjt.splitwise.server.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.notifications.NotificationCenter;

import java.util.HashSet;
import java.util.Set;

public class User {
    private final String username;
    private final String firstName;
    private final String familyName;
    private final Set<String> groups = new HashSet<>();
    private NotificationCenter notificationCenter;

    public User(String username, String firstName, String familyName) {
        if (username == null || firstName == null || familyName == null) {
            throw new IllegalArgumentException("Username and names cannot be null");
        }

        this.firstName = firstName;
        this.familyName = familyName;
        this.username = username;
        notificationCenter = new NotificationCenter(username);
    }

    void setNotificationCenter(NotificationCenter notificationCenter) {
        this.notificationCenter = notificationCenter;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void addToGroup(String groupName) {
        if (groupName == null) {
            throw new IllegalArgumentException("Group name cannot be null");
        }

        groups.add(groupName);
    }

    public boolean isInGroup(String groupName) {
        if (groupName == null) {
            throw new IllegalArgumentException("Group name cannot be null");
        }

        return groups.contains(groupName);
    }

    public void addPaidDebt(DebtRecord debt) throws ServerErrorException {
        notificationCenter.addPaidDebt(debt);
    }

    public void addPartlyPaidDebt(DebtRecord debt) throws ServerErrorException {
        notificationCenter.addPartlyPaidDebt(debt);
    }

    public void addNewDebt(DebtRecord debt) throws ServerErrorException {
        notificationCenter.addNewDebt(debt);
    }

    public void addNewGroupDebt(DebtRecord debt, String groupName) throws ServerErrorException {
        notificationCenter.addNewGroupDebt(debt, groupName);
    }

    public String getNotifications() throws ServerErrorException {
        return notificationCenter.getNotifications();
    }
}
