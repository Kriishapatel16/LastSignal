
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import model.CheckInSettingsBean;
import util.DBConnection;

public class CheckInSettingsDAO {

    public CheckInSettingsBean findByUserId(int userId) throws SQLException {
        requireUserId(userId);

        String sql = "SELECT SETTING_ID, USER_ID, INTERVAL_HOURS, GRACE_MINUTES, "
                + "ESCALATION_DELAY, IS_ENABLED "
                + "FROM CHECKIN_SETTINGS WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                CheckInSettingsBean bean = map(rs);

                if (rs.next()) {
                    throw new SQLException(
                            "Multiple CHECKIN_SETTINGS rows exist for one user.");
                }

                return bean;
            }
        }
    }

    public void saveAndSynchronize(CheckInSettingsBean bean) throws SQLException {
        if (bean == null) {
            throw new IllegalArgumentException("Settings are required.");
        }

        requireUserId(bean.getUserId());

        try (Connection con = DBConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {
                CheckInDAO.lockUser(con, bean.getUserId());

                String update = "UPDATE CHECKIN_SETTINGS SET INTERVAL_HOURS = ?, "
                        + "GRACE_MINUTES = ?, ESCALATION_DELAY = ?, IS_ENABLED = ? "
                        + "WHERE USER_ID = ?";

                int changed;
                try (PreparedStatement ps = con.prepareStatement(update)) {
                    bindSettings(ps, bean);
                    ps.setInt(5, bean.getUserId());
                    changed = ps.executeUpdate();
                }

                if (changed == 0) {
                    String insert = "INSERT INTO CHECKIN_SETTINGS "
                            + "(USER_ID, INTERVAL_HOURS, GRACE_MINUTES, "
                            + "ESCALATION_DELAY, IS_ENABLED) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = con.prepareStatement(insert)) {
                        ps.setInt(1, bean.getUserId());
                        ps.setInt(2, bean.getIntervalHours());
                        ps.setInt(3, bean.getGraceMinutes());
                        ps.setInt(4, bean.getEscalationDelay());
                        ps.setInt(5, bean.isEnabled() ? 1 : 0);
                        ps.executeUpdate();
                    }
                }

                // Every Save invalidates previous email links. When monitoring is
                // enabled, it also starts a fresh interval from the current time,
                // even when the submitted values are identical to the old values.
                String invalidateTokens =
                        "UPDATE ACTION_TOKENS SET USED_AT = CURRENT_TIMESTAMP "
                        + "WHERE USER_ID = ? AND USED_AT IS NULL";
                try (PreparedStatement ps = con.prepareStatement(invalidateTokens)) {
                    ps.setInt(1, bean.getUserId());
                    ps.executeUpdate();
                }

                if (bean.isEnabled()) {
                    synchronizePendingSchedule(con, bean);
                }

                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private void synchronizePendingSchedule(
            Connection con, CheckInSettingsBean bean) throws SQLException {

        Timestamp now = Timestamp.from(Instant.now());

        long dueMillis = now.getTime() + intervalMillis(bean.getIntervalHours());

        Timestamp due = new Timestamp(dueMillis);

        Timestamp graceDeadline = new Timestamp(
                dueMillis + bean.getGraceMinutes() * 60_000L);

        String update = "UPDATE CHECKIN_RECORDS SET SCHEDULED_AT = ?, "
                + "GRACE_DEADLINE = ?, REMINDER_SENT_AT = NULL "
                + "WHERE USER_ID = ? AND STATUS = 'PENDING'";

        int changed;

        try (PreparedStatement ps = con.prepareStatement(update)) {
            ps.setTimestamp(1, due);
            ps.setTimestamp(2, graceDeadline);
            ps.setInt(3, bean.getUserId());
            changed = ps.executeUpdate();
        }

        if (changed == 0) {
            CheckInDAO.insertPending(
                    con,
                    bean.getUserId(),
                    bean.getIntervalHours(),
                    bean.getGraceMinutes(),
                    now);
        }
    }

    private long intervalMillis(int intervalHours) {
        if (intervalHours == 1) {
            return 60_000L;
        }

        return intervalHours * 3_600_000L;
    }

    private void bindSettings(
            PreparedStatement ps, CheckInSettingsBean bean) throws SQLException {

        ps.setInt(1, bean.getIntervalHours());
        ps.setInt(2, bean.getGraceMinutes());
        ps.setInt(3, bean.getEscalationDelay());
        ps.setInt(4, bean.isEnabled() ? 1 : 0);
    }

    private CheckInSettingsBean map(ResultSet rs) throws SQLException {
        CheckInSettingsBean bean = new CheckInSettingsBean();

        bean.setSettingId(rs.getInt("SETTING_ID"));
        bean.setUserId(rs.getInt("USER_ID"));
        bean.setIntervalHours(rs.getInt("INTERVAL_HOURS"));
        bean.setGraceMinutes(rs.getInt("GRACE_MINUTES"));
        bean.setEscalationDelay(rs.getInt("ESCALATION_DELAY"));
        bean.setEnabled(rs.getInt("IS_ENABLED") != 0);

        return bean;
    }

    private void requireUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid account.");
        }
    }
}
