package bg.sofia.uni.fmi.mjt.splitwise.server.passwords;

public record Password(String hash, byte[] salt) {
}
