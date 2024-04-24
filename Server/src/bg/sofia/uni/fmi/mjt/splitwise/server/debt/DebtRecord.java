package bg.sofia.uni.fmi.mjt.splitwise.server.debt;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;

public record DebtRecord(User debtor, User creditor, double amount, String reason) {
    private static final double ROUNDING_VALUE = 100.0;

    @Override
    public String toString() {
        return debtor.getUsername() + " " + creditor.getUsername() + " " + getRoundedValue(amount) + " " + reason;
    }

    private double getRoundedValue(double value) {
        return Math.round(value * ROUNDING_VALUE) / ROUNDING_VALUE;
    }

    public String visualizeDebt(String action) {
        User user = debtor;

        if (action.equals("You owe")) {
            user = creditor;
        }

        return user.getFirstName() + " " + user.getFamilyName() + " (" + user.getUsername() + "): " +
            action + " " + String.format("%.2f", amount) + " LV [" + reason + "]";
    }

    public double payAmount(double payment) throws NonPositiveAmountException {
        if (payment <= 0) {
            throw new NonPositiveAmountException("The amount cannot be 0 or less");
        }

        return getRoundedValue(amount - payment);
    }

    public String getSuccessfullyPaidDebt() {
        return String.format(" - %s [%s] approved your %.2f LV payment for %s", creditor.getFirstName(),
            creditor.getUsername(), amount, reason);
    }

    public String getPartlyPaidDebt() {
        return String.format(" - You still owe %s [%s] %.2f LV for %s", creditor.getFirstName(), creditor.getUsername(),
            amount, reason);
    }
}
