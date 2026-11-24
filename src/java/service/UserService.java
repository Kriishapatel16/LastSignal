package service;

import dao.UserDAO;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import model.UserBean;
import util.PasswordUtil;

/** Validation and authentication rules for user accounts. */
public final class UserService {

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{M} .'-]+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9() .-]{7,20}$");

    private final UserDAO userDAO = new UserDAO();

    public String register(String name, String email, String password, String phone) {
        String cleanName = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        String cleanEmail = normalizeEmail(email);
        String cleanPhone = phone == null ? "" : phone.trim();

        if (cleanName.length() < 2 || cleanName.length() > 100 || !NAME.matcher(cleanName).matches()) {
            return "Enter a valid name (2–100 characters).";
        }
        if (cleanEmail.length() > 254 || !EMAIL.matcher(cleanEmail).matches()) {
            return "Enter a valid email address.";
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            return "Password must contain 8–128 characters.";
        }
        if (!cleanPhone.isEmpty() && !PHONE.matcher(cleanPhone).matches()) {
            return "Enter a valid phone number or leave it blank.";
        }

        try {
            if (userDAO.emailExists(cleanEmail)) {
                return "An account with this email already exists.";
            }
            String passwordHash = PasswordUtil.hashPassword(password);
            UserBean user = new UserBean(cleanName, cleanEmail, passwordHash, cleanPhone);
            return userDAO.registerUser(user) > 0
                    ? "Registration successful. You can now log in."
                    : "Registration could not be completed.";
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                return "An account with this email already exists.";
            }
            LOGGER.log(Level.SEVERE, "Account registration failed.", exception);
            return "Registration is temporarily unavailable. Please try again later.";
        }
    }

    /** Returns an active account on success, otherwise null. */
    public UserBean login(String email, String password) {
        if (email == null || password == null || password.isEmpty()
                || email.length() > 254 || password.length() > 128) {
            return null;
        }

        try {
            UserBean user = userDAO.findByEmail(normalizeEmail(email));
            if (user == null || user.getPasswordHash() == null
                    || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                return null;
            }
            return PasswordUtil.verifyPassword(password, user.getPasswordHash()) ? user : null;
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Authentication database operation failed.", exception);
            throw new IllegalStateException("Authentication service is unavailable.", exception);
        }
    }

    public UserBean getUserById(int userId) {
        if (userId <= 0) {
            return null;
        }
        try {
            return userDAO.findById(userId);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "User lookup failed.", exception);
            throw new IllegalStateException("User service is unavailable.", exception);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
