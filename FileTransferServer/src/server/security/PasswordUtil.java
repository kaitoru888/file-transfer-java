package server.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();
    private PasswordUtil(){}

    public static String hashNew(String password) {
        String salt = randomSalt(10);
        return salt + ":" + sha256Hex(salt + password);
    }

public static boolean verify(String password, String stored) {
    if (stored == null) return false;

    // Nếu DB đang lưu password plain (ví dụ 123456)
    if (!stored.contains(":")) {
        return stored.equals(password);
    }

    String[] parts = stored.split(":", 2);
    String salt = parts[0];
    String hash = parts[1];

    return sha256Hex(salt + password).equalsIgnoreCase(hash);
}
    private static String randomSalt(int len) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i=0;i<len;i++) sb.append(alphabet.charAt(RNG.nextInt(alphabet.length())));
        return sb.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}