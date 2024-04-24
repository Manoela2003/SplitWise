package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.InvalidAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.groups.GroupManager;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.AmountUtils;

public class SplitGroupCommand extends Command {
    private final String username;
    private final String groupName;
    private final String amount;
    private final String reason;

    public SplitGroupCommand(String username, String groupName, String amount, String reason) {
        this.username = username;
        this.groupName = groupName;
        this.amount = amount;
        this.reason = reason;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (username == null || groupName == null || amount == null || reason == null) {
            throw new IllegalArgumentException("Username, group name, amount and reason cannot be null");
        }

        double debtMoney;
        try {
            debtMoney = AmountUtils.checkAmount(amount);
        } catch (InvalidAmountException e) {
            return e.getMessage();
        }

        if (!groupManager.existsGroup(groupName)) {
            return String.format("Group %s doesn't exist", groupName);
        }

        if (!UserRepository.toUser(username).isInGroup(groupName)) {
            return "You are not part of the following group: " + groupName;
        }

        try {
            debtManager.splitGroupBill(GroupManager.toGroup(groupName), username, debtMoney, reason);
        } catch (NonPositiveAmountException e) {
            return e.getMessage();
        }

        return "Amount successfully split";
    }
}
