package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;

import java.util.Set;

public class GetStatusCommand extends Command {
    private final String username;

    public GetStatusCommand(String username) {
        this.username = username;
    }

    @Override
    public String execute() {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        String status = getStatus();

        if (status.isEmpty()) {
            return "No notifications to be shown";
        }

        return status;
    }

    private String getStatus() {
        StringBuilder builder = new StringBuilder();

        Set<DebtRecord> moneyOwed = debtManager.getMoneyOwed(username);
        Set<DebtRecord> owesMoney = debtManager.getOwesMoney(username);

        if ((moneyOwed != null && !moneyOwed.isEmpty()) || (owesMoney != null && !owesMoney.isEmpty())) {
            builder.append(" * Friends:\n");

            builder.append(printDebts(moneyOwed, "Owes you"));
            builder.append(printDebts(owesMoney, "You owe"));
        }

        appendGroupDebts(builder);

        return builder.toString();
    }

    private void appendGroupDebts(StringBuilder builder) {
        User user = UserRepository.toUser(username);
        Set<String> groups = user.getGroups();

        if (groups != null && !groups.isEmpty()) {
            for (String group : groups) {

                Set<DebtRecord> groupMoneyOwed = debtManager.getGroupMoneyOwed(group, username);
                Set<DebtRecord> groupOwesMoney = debtManager.getGroupOwesMoney(group, username);

                if ((groupMoneyOwed != null && !groupMoneyOwed.isEmpty()) ||
                    (groupOwesMoney != null && !groupOwesMoney.isEmpty())) {

                    builder.append('\n').append(" * Group: ").append(group).append('\n');
                    builder.append(printDebts(groupMoneyOwed, "Owes you"));
                    builder.append(printDebts(groupOwesMoney, "You owe"));
                }
            }
        }
    }

    private String printDebts(Set<DebtRecord> debts, String action) {
        if (debts == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();

        for (DebtRecord debt : debts) {
            builder.append(" - ").append(debt.visualizeDebt(action)).append("\n");
        }

        return builder.toString();
    }
}
