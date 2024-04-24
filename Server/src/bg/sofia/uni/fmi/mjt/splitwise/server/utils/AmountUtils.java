package bg.sofia.uni.fmi.mjt.splitwise.server.utils;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.InvalidAmountException;

public class AmountUtils {
    public static double checkAmount(String args) throws InvalidAmountException {
        if (args == null) {
            throw new IllegalArgumentException("Args cannot be null");
        }

        double amount;
        try {
            amount = Double.parseDouble(args);
            return amount;
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("The amount should be a number");
        }
    }
}
