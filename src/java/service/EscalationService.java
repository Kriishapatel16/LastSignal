package service;

import dao.ContactDAO;
import dao.NotificationDAO;
import dao.CheckInDAO;
import email.EmailTemplates;
import email.EmailWorkerService;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.CheckInBean;
import model.CheckInSettingsBean;
import model.ContactBean;
import model.UserBean;
import util.DBConnection;

public final class EscalationService {
    private static final Logger LOGGER = Logger.getLogger(EscalationService.class.getName());

    private final CheckInDAO checkInDAO = new CheckInDAO();
    private final ContactDAO contactDAO = new ContactDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final CheckInSettingsService settingsService = new CheckInSettingsService();
    private final UserService userService = new UserService();
    private final EmailWorkerService mailer = new EmailWorkerService();

    public void processDueEscalations() throws SQLException {
        List<CheckInBean> due = checkInDAO.findDueEscalations();
        for (CheckInBean checkIn : due) {
            try {
                processOne(checkIn);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING,
                        "Escalation processing failed for check-in " + checkIn.getCheckInId(), ex);
            }
        }
    }

    private void processOne(CheckInBean checkIn) throws Exception {
        if (!checkInDAO.isStillPending(checkIn.getCheckInId())) return;

        CheckInSettingsBean settings = settingsService.getSettings(checkIn.getUserId());
        if (!settings.isEnabled()) return;

        Timestamp lastSent = notificationDAO.findLastSuccessfulEscalation(
                checkIn.getCheckInId(), checkIn.getScheduledAt());
        if (lastSent != null) {
            long nextAllowed = lastSent.getTime() + settings.getEscalationDelay() * 60000L;
            if (Instant.now().toEpochMilli() < nextAllowed) return;
        }

        UserBean account = userService.getUserById(checkIn.getUserId());
        if (account == null) return;

        List<ContactBean> contacts = contactDAO.getContactsByUserId(checkIn.getUserId());
        for (ContactBean contact : contacts) {
            if (!contact.isActive()) continue;

            // Include the current schedule timestamp so a settings Save that
            // resets the same pending record starts a new notification cycle.
            String eventKey = "C" + checkIn.getCheckInId()
                    + "-T" + checkIn.getScheduledAt().getTime()
                    + "-P" + contact.getContactId();

            if (notificationDAO.wasEscalationSent(eventKey)) continue;

            boolean reserved = notificationDAO.reserveEscalation(
                    checkIn.getCheckInId(), checkIn.getUserId(),
                    contact.getContactId(), contact.getEmail(),
                    contact.getPriorityLevel(), eventKey);

            // Do not skip a failed/in-flight higher-priority contact and notify
            // a lower-priority contact out of order.
            if (!reserved) return;

            // Hold the user's row lock during SMTP dispatch. Disabling monitoring
            // or confirming the check-in must wait for this in-flight dispatch to finish.
            try (Connection con = DBConnection.getConnection()) {
                boolean oldAutoCommit = con.getAutoCommit();
                con.setAutoCommit(false);
                try {
                    CheckInDAO.lockUser(con, checkIn.getUserId());
                    if (!CheckInDAO.isEligibleForEscalation(
                            con, checkIn.getCheckInId(), checkIn.getUserId())) {
                        notificationDAO.completeEscalation(con, eventKey, false,
                                "Check-in was confirmed or monitoring was disabled before dispatch.");
                        con.commit();
                        return;
                    }

                    boolean delivered = false;
                    String failure = null;
                    try {
                        String subject = "Missed safety check-in: " + account.getName();
                        String body = EmailTemplates.trustedContactAlert(
                                contact.getContactName(), account.getName(),
                                checkIn.getScheduledAt(), contact.getPriorityLevel());
                        mailer.send(contact.getEmail(), subject, body);
                        delivered = true;
                    } catch (Exception mailFailure) {
                        failure = mailFailure.getMessage();
                        LOGGER.log(Level.WARNING,
                                "Unable to send LastSignal escalation to contact ID "
                                        + contact.getContactId(), mailFailure);
                    }
                    notificationDAO.completeEscalation(con, eventKey, delivered, failure);
                    con.commit();
                } catch (Exception dispatchFailure) {
                    con.rollback();
                    throw dispatchFailure;
                } finally {
                    con.setAutoCommit(oldAutoCommit);
                }
            }
            return; // One priority contact per escalation interval.
        }
    }
}
