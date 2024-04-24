package bg.sofia.uni.fmi.mjt.splitwise.server.utils;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.InvalidAmountException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AmountUtilsTest {
    @Test
    void testCheckAmountWhenNotNumber() {
        assertThrows(InvalidAmountException.class, () -> AmountUtils.checkAmount("test"),
            "Expected InvalidAmountException when the passed amount is not number");
    }

    @Test
    void testCheckAmountWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> AmountUtils.checkAmount(null),
            "Expected IllegalArgumentException when the passed amount is null");
    }

    @Test
    void testCheckAmountReturnsDouble() throws InvalidAmountException {
        double expectedResult = 15;
        double returnedResult = AmountUtils.checkAmount("15");

        assertEquals(expectedResult, returnedResult, "Expected a double of 15 to be returned but it was not");
    }
}

