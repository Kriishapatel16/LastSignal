package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import model.ActionTokenBean;
import util.DBConnection;
import util.TokenUtil;

/** Database operations for one-time email confirmation tokens. */
public final class ActionTokenDAO {

    /** Creates a new token and invalidates older unused tokens for this check-in. */
    public boolean createToken(int userId, int checkInId, String tokenHash)
            throws SQLException {
        if (userId <= 0 || checkInId <= 0 || !TokenUtil.isValidHash(tokenHash)) {
            throw new IllegalArgumentException("Invalid confirmation token request.");
        }

        try (Connection con = DBConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                CheckInDAO.lockUser(con, userId);

                Timestamp expiresAt;
                String eligible = "SELECT C.GRACE_DEADLINE FROM CHECKIN_RECORDS C "
                        + "JOIN CHECKIN_SETTINGS S ON S.USER_ID = C.USER_ID "
                        + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                        + "WHERE C.CHECKIN_ID = ? AND C.USER_ID = ? AND C.STATUS = 'PENDING' "
                        + "AND C.SCHEDULED_AT <= CURRENT_TIMESTAMP AND C.GRACE_DEADLINE > CURRENT_TIMESTAMP "
                        + "AND C.REMINDER_SENT_AT IS NULL AND S.IS_ENABLED = 1 AND U.STATUS = 'ACTIVE'";
                try (PreparedStatement ps = con.prepareStatement(eligible)) {
                    ps.setInt(1, checkInId);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        expiresAt = rs.getTimestamp("GRACE_DEADLINE");
                    }
                }

                String invalidate = "UPDATE ACTION_TOKENS SET USED_AT = CURRENT_TIMESTAMP "
                        + "WHERE USER_ID = ? AND CHECKIN_ID = ? AND USED_AT IS NULL";
                try (PreparedStatement ps = con.prepareStatement(invalidate)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, checkInId);
                    ps.executeUpdate();
                }

                String insert = "INSERT INTO ACTION_TOKENS "
                        + "(USER_ID, CHECKIN_ID, TOKEN_HASH, EXPIRES_AT, USED_AT) "
                        + "VALUES (?, ?, ?, ?, NULL)";
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, checkInId);
                    ps.setString(3, tokenHash);
                    ps.setTimestamp(4, expiresAt);
                    ps.executeUpdate();
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

    /** Read-only validation for rendering the confirmation page; GET never consumes a token. */
    public ActionTokenBean findValidToken(String tokenHash) throws SQLException {
        if (!TokenUtil.isValidHash(tokenHash)) return null;
        String sql = "SELECT T.TOKEN_ID, T.USER_ID, T.CHECKIN_ID, T.EXPIRES_AT, T.USED_AT "
                + "FROM ACTION_TOKENS T JOIN CHECKIN_RECORDS C ON C.CHECKIN_ID = T.CHECKIN_ID "
                + "JOIN CHECKIN_SETTINGS S ON S.USER_ID = T.USER_ID "
                + "JOIN USERS U ON U.USER_ID = T.USER_ID "
                + "WHERE T.TOKEN_HASH = ? AND T.USED_AT IS NULL "
                + "AND T.EXPIRES_AT > CURRENT_TIMESTAMP AND C.USER_ID = T.USER_ID "
                + "AND C.STATUS = 'PENDING' AND C.SCHEDULED_AT <= CURRENT_TIMESTAMP "
                + "AND C.GRACE_DEADLINE >= CURRENT_TIMESTAMP AND S.IS_ENABLED = 1 "
                + "AND U.STATUS = 'ACTIVE'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ActionTokenBean bean = map(rs);
                return rs.next() ? null : bean;
            }
        }
    }

    /** Atomically consumes the token, confirms its check-in, and creates the next check-in. */
    public boolean confirmAndScheduleNext(String tokenHash) throws SQLException {
        if (!TokenUtil.isValidHash(tokenHash)) return false;

        try (Connection con = DBConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int userId;
                String ownerSql = "SELECT USER_ID FROM ACTION_TOKENS WHERE TOKEN_HASH = ?";
                try (PreparedStatement ps = con.prepareStatement(ownerSql)) {
                    ps.setString(1, tokenHash);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        userId = rs.getInt("USER_ID");
                    }
                }

                CheckInDAO.lockUser(con, userId);

                ActionTokenBean token;
                String tokenSql = "SELECT TOKEN_ID, USER_ID, CHECKIN_ID, EXPIRES_AT, USED_AT "
                        + "FROM ACTION_TOKENS WHERE TOKEN_HASH = ? AND USER_ID = ? "
                        + "AND USED_AT IS NULL AND EXPIRES_AT > CURRENT_TIMESTAMP FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(tokenSql)) {
                    ps.setString(1, tokenHash);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        token = map(rs);
                    }
                }

                int intervalHours;
                int graceMinutes;
                String settingsSql = "SELECT S.INTERVAL_HOURS, S.GRACE_MINUTES "
                        + "FROM CHECKIN_SETTINGS S JOIN USERS U ON U.USER_ID = S.USER_ID "
                        + "WHERE S.USER_ID = ? AND S.IS_ENABLED = 1 AND U.STATUS = 'ACTIVE'";
                try (PreparedStatement ps = con.prepareStatement(settingsSql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        intervalHours = rs.getInt("INTERVAL_HOURS");
                        graceMinutes = rs.getInt("GRACE_MINUTES");
                    }
                }

                String checkInSql = "SELECT CHECKIN_ID FROM CHECKIN_RECORDS "
                        + "WHERE CHECKIN_ID = ? AND USER_ID = ? AND STATUS = 'PENDING' "
                        + "AND SCHEDULED_AT <= CURRENT_TIMESTAMP "
                        + "AND GRACE_DEADLINE >= CURRENT_TIMESTAMP FOR UPDATE";
                try (PreparedStatement ps = con.prepareStatement(checkInSql)) {
                    ps.setInt(1, token.getCheckInId());
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                    }
                }

                String consume = "UPDATE ACTION_TOKENS SET USED_AT = CURRENT_TIMESTAMP "
                        + "WHERE TOKEN_ID = ? AND USED_AT IS NULL AND EXPIRES_AT > CURRENT_TIMESTAMP";
                try (PreparedStatement ps = con.prepareStatement(consume)) {
                    ps.setInt(1, token.getTokenId());
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }

                String confirm = "UPDATE CHECKIN_RECORDS SET CONFIRMED_AT = CURRENT_TIMESTAMP, STATUS = 'CONFIRMED' "
                        + "WHERE CHECKIN_ID = ? AND USER_ID = ? AND STATUS = 'PENDING'";
                try (PreparedStatement ps = con.prepareStatement(confirm)) {
                    ps.setInt(1, token.getCheckInId());
                    ps.setInt(2, userId);
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }

                String invalidate = "UPDATE ACTION_TOKENS SET USED_AT = CURRENT_TIMESTAMP "
                        + "WHERE USER_ID = ? AND CHECKIN_ID = ? AND USED_AT IS NULL";
                try (PreparedStatement ps = con.prepareStatement(invalidate)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, token.getCheckInId());
                    ps.executeUpdate();
                }

                CheckInDAO.insertPending(con, userId, intervalHours, graceMinutes,
                        new Timestamp(System.currentTimeMillis()));
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

    private ActionTokenBean map(ResultSet rs) throws SQLException {
        ActionTokenBean bean = new ActionTokenBean();
        bean.setTokenId(rs.getInt("TOKEN_ID"));
        bean.setUserId(rs.getInt("USER_ID"));
        bean.setCheckInId(rs.getInt("CHECKIN_ID"));
        bean.setExpiresAt(rs.getTimestamp("EXPIRES_AT"));
        bean.setUsedAt(rs.getTimestamp("USED_AT"));
        return bean;
    }
}
