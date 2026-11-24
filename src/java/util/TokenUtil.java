package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** Generates high-entropy email action tokens and stores only their SHA-256 hashes. */
public final class TokenUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern HASH_PATTERN = Pattern.compile("^[a-f0-9]{64}$");

    private TokenUtil() { }

    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String token) {
        if (token == null || token.length() < 40 || token.length() > 64) {
            throw new IllegalArgumentException("Invalid action token.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    public static boolean isValidHash(String hash) {
        return hash != null && HASH_PATTERN.matcher(hash).matches();
    }
}
