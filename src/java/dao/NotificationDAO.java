
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import model.NotificationBean;
import util.DBConnection;

public final class NotificationDAO {

    private static final long RETRY_AFTER_MILLIS = 5L * 60L * 1000L;

    public boolean wasEscalationSent(String eventKey) throws SQLException {
        String sql = "SELECT DELIVERY_STATUS FROM NOTIFICATION_LOGS WHERE EVENT_KEY = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, eventKey);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        && "SENT".equalsIgnoreCase(rs.getString("DELIVERY_STATUS"));
            }
        }
    }

    public Timestamp findLastSuccessfulEscalation(
            int checkInId, Timestamp currentSchedule) throws SQLException {

        String sql = "SELECT MAX(SENT_AT) AS LAST_SENT FROM NOTIFICATION_LOGS "
                + "WHERE CHECKIN_ID = ? AND NOTIFICATION_TYPE = 'ALERT' "
                + "AND DELIVERY_STATUS = 'SENT' AND SENT_AT >= ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, checkInId);
            ps.setTimestamp(2, currentSchedule);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp("LAST_SENT") : null;
            }
        }
    }

    public boolean reserveEscalation(
            int checkInId,
            int userId,
            int contactId,
            String recipient,
            int priority,
            String eventKey) throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {
                CheckInDAO.lockUser(con, userId);

                String activeSql =
                        "SELECT C.CHECKIN_ID FROM CHECKIN_RECORDS C "
                        + "JOIN CHECKIN_SETTINGS S ON S.USER_ID = C.USER_ID "
                        + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                        + "JOIN TRUSTED_CONTACTS T ON T.USER_ID = C.USER_ID "
                        + "WHERE C.CHECKIN_ID = ? AND C.USER_ID = ? "
                        + "AND C.STATUS = 'PENDING' "
                        + "AND S.IS_ENABLED = 1 "
                        + "AND U.STATUS = 'ACTIVE' "
                        + "AND C.GRACE_DEADLINE <= CURRENT_TIMESTAMP "
                        + "AND T.CONTACT_ID = ? "
                        + "AND T.IS_ACTIVE = 1 "
                        + "AND T.PRIORITY_LEVEL = ? "
                        + "AND T.EMAIL = ?";

                try (PreparedStatement ps = con.prepareStatement(activeSql)) {

                    ps.setInt(1, checkInId);
                    ps.setInt(2, userId);
                    ps.setInt(3, contactId);
                    ps.setInt(4, priority);
                    ps.setString(5, recipient);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                    }
                }

                String existingSql =
                        "SELECT DELIVERY_STATUS, SENT_AT "
                        + "FROM NOTIFICATION_LOGS WHERE EVENT_KEY = ?";

                String state = null;
                Timestamp attemptedAt = null;

                try (PreparedStatement ps = con.prepareStatement(existingSql)) {

                    ps.setString(1, eventKey);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            state = rs.getString("DELIVERY_STATUS");
                            attemptedAt = rs.getTimestamp("SENT_AT");
                        }
                    }
                }

                Timestamp now = Timestamp.from(Instant.now());

                if ("SENT".equalsIgnoreCase(state)) {
                    con.commit();
                    return false;
                }

                if (("PENDING".equalsIgnoreCase(state)
                        || "FAILED".equalsIgnoreCase(state))
                        && attemptedAt != null
                        && now.getTime() - attemptedAt.getTime()
                        < RETRY_AFTER_MILLIS) {

                    con.commit();
                    return false;
                }

                if (state == null) {

                    String insert =
                            "INSERT INTO NOTIFICATION_LOGS "
                            + "(CHECKIN_ID, CONTACT_ID, RECIPIENT_EMAIL, "
                            + "NOTIFICATION_TYPE, ESCALATION_LEVEL, EVENT_KEY, "
                            + "SENT_AT, DELIVERY_STATUS, ERROR_MESSAGE) "
                            + "VALUES (?, ?, ?, 'ALERT', ?, ?, "
                            + "CURRENT_TIMESTAMP, 'PENDING', NULL)";

                    try (PreparedStatement ps = con.prepareStatement(insert)) {

                        ps.setInt(1, checkInId);
                        ps.setInt(2, contactId);
                        ps.setString(3, recipient);
                        ps.setInt(4, priority);
                        ps.setString(5, eventKey);

                        ps.executeUpdate();
                    }

                } else {

                    String update =
                            "UPDATE NOTIFICATION_LOGS SET "
                            + "RECIPIENT_EMAIL = ?, "
                            + "DELIVERY_STATUS = 'PENDING', "
                            + "SENT_AT = CURRENT_TIMESTAMP, "
                            + "ERROR_MESSAGE = NULL "
                            + "WHERE EVENT_KEY = ?";

                    try (PreparedStatement ps = con.prepareStatement(update)) {

                        ps.setString(1, recipient);
                        ps.setString(2, eventKey);

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Notification retry reservation failed.");
                        }
                    }
                }

                con.commit();
                return true;

            } catch (SQLException | RuntimeException ex) {

                con.rollback();
                throw ex;

            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public void completeEscalation(
            String eventKey, boolean delivered, String error)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {
            completeEscalation(con, eventKey, delivered, error);
        }
    }

    public void completeEscalation(
            Connection con,
            String eventKey,
            boolean delivered,
            String error) throws SQLException {

        String sql =
                "UPDATE NOTIFICATION_LOGS SET DELIVERY_STATUS = ?, "
                + "SENT_AT = CURRENT_TIMESTAMP, ERROR_MESSAGE = ? "
                + "WHERE EVENT_KEY = ? AND DELIVERY_STATUS = 'PENDING'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, delivered ? "SENT" : "FAILED");
            ps.setString(2, delivered ? null : truncate(error, 250));
            ps.setString(3, eventKey);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Notification delivery result could not be recorded.");
            }
        }
    }

    public List<NotificationBean> findRecentNotifications(int limit)
            throws SQLException {

        int safeLimit = Math.max(1, Math.min(limit, 200));

        String sql =
                "SELECT NOTIFICATION_ID, CHECKIN_ID, CONTACT_ID, "
                + "RECIPIENT_EMAIL, NOTIFICATION_TYPE, ESCALATION_LEVEL, "
                + "EVENT_KEY, SENT_AT, DELIVERY_STATUS, ERROR_MESSAGE "
                + "FROM NOTIFICATION_LOGS ORDER BY SENT_AT DESC";

        List<NotificationBean> rows = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next() && rows.size() < safeLimit) {

                NotificationBean bean = new NotificationBean();

                bean.setNotificationId(rs.getInt("NOTIFICATION_ID"));
                bean.setCheckInId(rs.getInt("CHECKIN_ID"));
                bean.setContactId(rs.getInt("CONTACT_ID"));
                bean.setRecipientEmail(rs.getString("RECIPIENT_EMAIL"));
                bean.setNotificationType(rs.getString("NOTIFICATION_TYPE"));
                bean.setEscalationLevel(rs.getInt("ESCALATION_LEVEL"));
                bean.setEventKey(rs.getString("EVENT_KEY"));
                bean.setSentAt(rs.getTimestamp("SENT_AT"));
                bean.setDeliveryStatus(rs.getString("DELIVERY_STATUS"));
                bean.setErrorMessage(rs.getString("ERROR_MESSAGE"));

                rows.add(bean);
            }
        }

        return rows;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "Email delivery failed.";
        }

        return value.length() <= max
                ? value
                : value.substring(0, max);
    }
}
