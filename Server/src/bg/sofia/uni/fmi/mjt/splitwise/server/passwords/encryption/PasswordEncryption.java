package bg.sofia.uni.fmi.mjt.splitwise.server.passwords.encryption;

import bg.sofia.uni.fmi.mjt.splitwise.server.exceptions.ServerErrorException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class PasswordEncryption {
    private static final int BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 128;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String HASHING_ERROR = "A problem occurred while hashing the password";

    public static String hashPassword(String password, byte[] salt) throws ServerErrorException {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }

        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(hash);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new ServerErrorException(HASHING_ERROR, e);
        }
    }

    public static byte[] getSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[BYTES];
        random.nextBytes(salt);
        return salt;
    }
}
