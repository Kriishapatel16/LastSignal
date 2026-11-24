package service;

import dao.CheckInDAO;
import java.sql.SQLException;
import model.CheckInBean;

/** Read-only check-in queries used by the authenticated dashboard. */
public final class CheckInService {
    private final CheckInDAO checkInDAO = new CheckInDAO();

    public CheckInBean getCurrentCheckIn(int userId) throws SQLException {
        requireUser(userId);
        return checkInDAO.findCurrentPending(userId);
    }

    public CheckInBean getLatestCheckIn(int userId) throws SQLException {
        requireUser(userId);
        return checkInDAO.findLatestConfirmed(userId);
    }

    private void requireUser(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid account.");
        }
    }
}
