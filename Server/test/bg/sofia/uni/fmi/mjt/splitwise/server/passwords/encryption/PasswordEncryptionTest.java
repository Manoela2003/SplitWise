package bg.sofia.uni.fmi.mjt.splitwise.server.passwords.encryption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PasswordEncryptionTest {
    @Test
    void testHashPasswordWhenPasswordIsNull() {
        assertThrows(IllegalArgumentException.class, () -> PasswordEncryption.hashPassword(null, null),
            "Expected IllegalArgumentException when password is null");
    }
}
