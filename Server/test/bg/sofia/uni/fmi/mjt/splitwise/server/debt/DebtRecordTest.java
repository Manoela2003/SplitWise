package bg.sofia.uni.fmi.mjt.splitwise.server.debt;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.NonPositiveAmountException;
import bg.sofia.uni.fmi.mjt.splitwise.server.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DebtRecordTest {
    DebtRecord debt = new DebtRecord(mock(User.class), mock(User.class), 15, "reason");

    @Test
    void testPayAmountWhenAmountIsNonPositive() {
        assertThrows(NonPositiveAmountException.class, () -> debt.payAmount(-10),
            "Expected NonPositiveAmountException to be thrown when the amount is a non positive number");
    }

    @Test
    void testPayAmount() throws NonPositiveAmountException {
        assertEquals(5, debt.payAmount(10),
            "Expected the returned amount to be 5 but it was not");
    }
}
