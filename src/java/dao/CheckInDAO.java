
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.CheckInBean;
import util.DBConnection;

public class CheckInDAO {

    public CheckInBean findCurrentPending(int userId) throws SQLException {
        requireUserId(userId);

        String sql = "SELECT CHECKIN_ID, USER_ID, SCHEDULED_AT, GRACE_DEADLINE, "
                + "REMINDER_SENT_AT, CONFIRMED_AT, STATUS FROM CHECKIN_RECORDS "
                + "WHERE USER_ID = ? AND STATUS = 'PENDING' ORDER BY SCHEDULED_AT ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public CheckInBean findLatestConfirmed(int userId) throws SQLException {
        requireUserId(userId);

        String sql = "SELECT CHECKIN_ID, USER_ID, SCHEDULED_AT, GRACE_DEADLINE, "
                + "REMINDER_SENT_AT, CONFIRMED_AT, STATUS FROM CHECKIN_RECORDS "
                + "WHERE USER_ID = ? AND CONFIRMED_AT IS NOT NULL "
                + "ORDER BY CONFIRMED_AT DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<CheckInBean> findDueReminders() throws SQLException {
        String sql = "SELECT C.CHECKIN_ID, C.USER_ID, C.SCHEDULED_AT, C.GRACE_DEADLINE, "
                + "C.REMINDER_SENT_AT, C.CONFIRMED_AT, C.STATUS "
                + "FROM CHECKIN_RECORDS C JOIN CHECKIN_SETTINGS S ON S.USER_ID = C.USER_ID "
                + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                + "WHERE C.STATUS = 'PENDING' AND S.IS_ENABLED = 1 AND U.STATUS = 'ACTIVE' "
                + "AND C.SCHEDULED_AT <= CURRENT_TIMESTAMP AND C.REMINDER_SENT_AT IS NULL "
                + "ORDER BY C.SCHEDULED_AT";

        List<CheckInBean> result = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        }

        return result;
    }

    public List<CheckInBean> findDueEscalations() throws SQLException {
        String sql = "SELECT C.CHECKIN_ID, C.USER_ID, C.SCHEDULED_AT, C.GRACE_DEADLINE, "
                + "C.REMINDER_SENT_AT, C.CONFIRMED_AT, C.STATUS "
                + "FROM CHECKIN_RECORDS C JOIN CHECKIN_SETTINGS S ON S.USER_ID = C.USER_ID "
                + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                + "WHERE C.STATUS = 'PENDING' AND S.IS_ENABLED = 1 AND U.STATUS = 'ACTIVE' "
                + "AND C.GRACE_DEADLINE <= CURRENT_TIMESTAMP ORDER BY C.GRACE_DEADLINE";

        List<CheckInBean> result = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        }

        return result;
    }

    public boolean isEligibleForReminder(int checkInId, int userId)
            throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return isEligibleForReminder(con, checkInId, userId);
        }
    }

    public static boolean isEligibleForReminder(
            Connection con, int checkInId, int userId) throws SQLException {

        String sql = "SELECT C.CHECKIN_ID FROM CHECKIN_RECORDS C "
                + "JOIN CHECKIN_SETTINGS S ON S.USER_ID = C.USER_ID "
                + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                + "WHERE C.CHECKIN_ID = ? AND C.USER_ID = ? AND C.STATUS = 'PENDING' "
                + "AND C.SCHEDULED_AT <= CURRENT_TIMESTAMP AND C.REMINDER_SENT_AT IS NULL "
                + "AND S.IS_ENABLED = 1 AND U.STATUS = 'ACTIVE'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, checkInId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean markReminderSent(Connection con, int checkInId)
            throws SQLException {

        String sql = "UPDATE CHECKIN_RECORDS SET REMINDER_SENT_AT = CURRENT_TIMESTAMP "
                + "WHERE CHECKIN_ID = ? AND STATUS = 'PENDING' "
                + "AND REMINDER_SENT_AT IS NULL AND SCHEDULED_AT <= CURRENT_TIMESTAMP";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, checkInId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean isStillPending(int checkInId) throws SQLException {
        String sql = "SELECT CHECKIN_ID FROM CHECKIN_RECORDS "
                + "WHERE CHECKIN_ID = ? AND STATUS = 'PENDING'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, checkInId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isEligibleForEscalation(int checkInId, int userId)
            throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return isEligibleForEscalation(con, checkInId, userId);
        }
    }

    public static boolean isEligibleForEscalation(
            Connection con, int checkInId, int userId) throws SQLException {

        String sql = "SELECT C.CHECKIN_ID FROM CHECKIN_RECORDS C "
                + "JOIN CHECKIN_SETTINGS S ON S.USER_ID = C.USER_ID "
                + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                + "WHERE C.CHECKIN_ID = ? AND C.USER_ID = ? AND C.STATUS = 'PENDING' "
                + "AND S.IS_ENABLED = 1 AND U.STATUS = 'ACTIVE' "
                + "AND C.GRACE_DEADLINE <= CURRENT_TIMESTAMP";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, checkInId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void lockUser(Connection con, int userId) throws SQLException {
        String sql = "SELECT USER_ID FROM USERS WHERE USER_ID = ? FOR UPDATE";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("User record not found.");
                }
            }
        }
    }

    static void insertPending(
            Connection con,
            int userId,
            int intervalHours,
            int graceMinutes,
            Timestamp base) throws SQLException {

        long intervalMillis = intervalHours == 1
                ? 60_000L
                : intervalHours * 3_600_000L;

        long dueMillis = base.getTime() + intervalMillis;

        Timestamp due = new Timestamp(dueMillis);

        Timestamp grace = new Timestamp(
                dueMillis + graceMinutes * 60_000L);

        String sql = "INSERT INTO CHECKIN_RECORDS "
                + "(USER_ID, SCHEDULED_AT, GRACE_DEADLINE, STATUS) "
                + "VALUES (?, ?, ?, 'PENDING')";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, due);
            ps.setTimestamp(3, grace);
            ps.executeUpdate();
        }
    }

    private CheckInBean map(ResultSet rs) throws SQLException {
        CheckInBean bean = new CheckInBean();

        bean.setCheckInId(rs.getInt("CHECKIN_ID"));
        bean.setUserId(rs.getInt("USER_ID"));
        bean.setScheduledAt(rs.getTimestamp("SCHEDULED_AT"));
        bean.setGraceDeadline(rs.getTimestamp("GRACE_DEADLINE"));
        bean.setReminderSentAt(rs.getTimestamp("REMINDER_SENT_AT"));
        bean.setConfirmedAt(rs.getTimestamp("CONFIRMED_AT"));
        bean.setStatus(rs.getString("STATUS"));

        return bean;
    }

    private void requireUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid account.");
        }
    }
}
