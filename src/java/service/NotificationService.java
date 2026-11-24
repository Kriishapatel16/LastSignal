package service;

import dao.ActionTokenDAO;
import dao.CheckInDAO;
import email.EmailTemplates;
import email.EmailWorkerService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import util.DBConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.CheckInBean;
import model.UserBean;
import util.TokenUtil;

/** Sends due reminders with a one-time email confirmation link. */
public final class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private static final long RETRY_DELAY_MS = 5L * 60L * 1000L;

    private final CheckInDAO checkInDAO = new CheckInDAO();
    private final ActionTokenDAO tokenDAO = new ActionTokenDAO();
    private final UserService userService = new UserService();
    private final EmailWorkerService mailer = new EmailWorkerService();
    private final ConcurrentMap<Integer, Long> lastAttempts = new ConcurrentHashMap<>();

    public void processDueReminders() throws SQLException {
        for (CheckInBean checkIn : checkInDAO.findDueReminders()) {
            long now = System.currentTimeMillis();
            Long previous = lastAttempts.putIfAbsent(checkIn.getCheckInId(), now);
            if (previous != null) {
                if (now - previous < RETRY_DELAY_MS) continue;
                if (!lastAttempts.replace(checkIn.getCheckInId(), previous, now)) continue;
            }

            try {
                if (!checkInDAO.isEligibleForReminder(checkIn.getCheckInId(), checkIn.getUserId())) {
                    lastAttempts.remove(checkIn.getCheckInId());
                    continue;
                }

                UserBean user = userService.getUserById(checkIn.getUserId());
                if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())
                        || user.getEmail() == null || user.getEmail().isBlank()) {
                    lastAttempts.remove(checkIn.getCheckInId());
                    continue;
                }

                String baseUrl = configuredBaseUrl();
                String rawToken = TokenUtil.generateToken();
                String tokenHash = TokenUtil.hashToken(rawToken);
                if (!tokenDAO.createToken(user.getUserId(), checkIn.getCheckInId(), tokenHash)) {
                    lastAttempts.remove(checkIn.getCheckInId());
                    continue;
                }

                String confirmationUrl = baseUrl + "/EmailConfirmationServlet?token="
                        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
                String body = EmailTemplates.checkInReminder(
                        user.getName(), checkIn.getScheduledAt(), confirmationUrl);

                // Hold the user's row lock during SMTP dispatch. A concurrent disable
                // request waits, so once disable succeeds no reminder is still in flight.
                try (Connection con = DBConnection.getConnection()) {
                    boolean oldAutoCommit = con.getAutoCommit();
                    con.setAutoCommit(false);
                    try {
                        CheckInDAO.lockUser(con, checkIn.getUserId());
                        if (!CheckInDAO.isEligibleForReminder(
                                con, checkIn.getCheckInId(), checkIn.getUserId())) {
                            con.rollback();
                            lastAttempts.remove(checkIn.getCheckInId());
                            continue;
                        }
                        String htmlBody = EmailTemplates.checkInReminderHtml(
                                user.getName(), checkIn.getScheduledAt(), confirmationUrl);
                        mailer.sendHtml(user.getEmail(), "Your LastSignal check-in is due", body, htmlBody);
                        if (!CheckInDAO.markReminderSent(con, checkIn.getCheckInId())) {
                            throw new SQLException("Reminder state changed before it could be recorded.");
                        }
                        con.commit();
                    } catch (Exception sendFailure) {
                        con.rollback();
                        throw sendFailure;
                    } finally {
                        con.setAutoCommit(oldAutoCommit);
                    }
                }
                lastAttempts.remove(checkIn.getCheckInId());
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING,
                        "Unable to send check-in reminder for check-in "
                                + checkIn.getCheckInId(), ex);
                // Keep the retry timestamp so repeated scheduler scans do not hammer SMTP.
            }
        }
    }

    private String configuredBaseUrl() {
        String configured = System.getenv("LASTSIGNAL_BASE_URL");
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing application configuration: LASTSIGNAL_BASE_URL");
        }
        String value = configured.trim().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("LASTSIGNAL_BASE_URL is invalid.", ex);
        }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("LASTSIGNAL_BASE_URL must be an absolute HTTP(S) application URL.");
        }
        return value;
    }
}
