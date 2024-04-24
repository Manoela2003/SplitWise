package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.InvalidAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.UserRepository;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.AmountUtils;

public class SplitCommand extends Command {
    private final String username;
    private final String friendUsername;
    private final String amount;
    private final String reason;

    public SplitCommand(String username, String friendUsername, String amount, String reason) {
        this.username = username;
        this.friendUsername = friendUsername;
        this.amount = amount;
        this.reason = reason;
    }

    @Override
    public String execute() throws ServerErrorException {
        if (username == null || friendUsername == null || amount == null || reason == null) {
            throw new IllegalArgumentException("Usernames, amount and reason cannot be null");
        }
        double debtMoney;
        try {
            debtMoney = AmountUtils.checkAmount(amount);
        } catch (InvalidAmountException e) {
            return e.getMessage();
        }

        if (!userRepository.containsUser(friendUsername)) {
            return "The following user doesn't exist: " + friendUsername;
        }
        if (username.equals(friendUsername)) {
            return "You cannot split the bill with yourself";
        }
        if (!friendsManager.hasFriend(username, UserRepository.toUser(friendUsername))) {
            return
                String.format("You don't have %s in your friend list. Add him first in order to split your bill.",
                    friendUsername);
        }

        try {
            debtManager.splitBill(username, friendUsername, debtMoney, reason);
            return String.format("Split %s between you and %s", debtMoney, friendUsername);
        } catch (NonPositiveAmountException e) {
            return e.getMessage();
        }
    }
}
