
package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {

    private static final String SESSION_KEY = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    public static String getOrCreateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);

        synchronized (session) {
            String token = (String) session.getAttribute(SESSION_KEY);

            if (token == null || token.isBlank()) {
                token = generateToken();
                session.setAttribute(SESSION_KEY, token);
            }

            return token;
        }
    }

    public static String rotateToken(HttpSession session) {
        synchronized (session) {
            String token = generateToken();
            session.setAttribute(SESSION_KEY, token);
            return token;
        }
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        Object stored = session.getAttribute(SESSION_KEY);
        String submitted = request.getParameter(SESSION_KEY);

        if (!(stored instanceof String) ||
                submitted == null ||
                submitted.isBlank()) {
            return false;
        }

        byte[] expected = ((String) stored)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] actual = submitted
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, actual);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
