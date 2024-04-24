package bg.sofia.uni.fmi.mjt.splitwise.server.command.user;

import bg.sofia.uni.fmi.mjt.splitwise.server.command.core.Command;
import bg.sofia.uni.fmi.mjt.splitwise.server.debt.DebtRecord;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.InvalidAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NoDebtsToBePaidException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.AmountUtils;
import bg.sofia.uni.fmi.mjt.splitwise.server.utils.FileUtils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class PayCommand extends Command {
    private final String username;
    private final String friend;
    private final String amount;

    public PayCommand(String username, String friend, String amount) {
        this.username = username;
        this.friend = friend;
        this.amount = amount;
    }

    @Override
    public String execute() throws ServerErrorException {
        String check = checkField();
        if (!check.isEmpty()) {
            return check;
        }

        double debtMoney;
        try {
            debtMoney = AmountUtils.checkAmount(amount);
        } catch (InvalidAmountException e) {
            return e.getMessage();
        }

        try {
            Set<DebtRecord> partlyPaidDebts = new HashSet<>();
            Set<DebtRecord> paidDebts = debtManager.pay(username, debtMoney, friend, partlyPaidDebts);
            Path fileName = Path.of(friend + "_transaction_history.txt");
            if (!paidDebts.isEmpty()) {
                FileUtils.writeToFile(fileName, System.lineSeparator(), paidDebts.toArray());
            }

            return showPaidDebts(paidDebts, partlyPaidDebts);
        } catch (NonPositiveAmountException | NoDebtsToBePaidException e) {
            return e.getMessage();
        }
    }

    private String checkField() {
        if (username == null || amount == null || friend == null) {
            throw new IllegalArgumentException("Username and friend cannot be null");
        }

        if (!userRepository.containsUser(friend)) {
            return "The following user doesn't exist: " + friend;
        }
        if (username.equals(friend)) {
            return "You cannot pay the bill yourself";
        }
        return "";
    }

    private String showPaidDebts(Set<DebtRecord> paidDebts, Set<DebtRecord> partlyPaidDebts) {
        StringBuilder builder = new StringBuilder();
        if (paidDebts != null && !paidDebts.isEmpty()) {
            builder.append(String.format(" * %s successfully paid for:\n", friend));

            for (DebtRecord debt : paidDebts) {
                builder.append(" - ").append(debt.reason()).append(" ").append(debt.amount()).append('\n');
            }
        }

        if (partlyPaidDebts != null && !partlyPaidDebts.isEmpty()) {
            DebtRecord debt = partlyPaidDebts.stream().findFirst().get();
            builder.append(
                String.format(" * %s partly paid for %s and still owes %s", friend, debt.reason(), debt.amount()));
        }

        return builder.toString();
    }
}
