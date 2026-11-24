package service;

import dao.ActionTokenDAO;
import java.sql.SQLException;
import model.ActionTokenBean;
import util.TokenUtil;

/** Public, token-based confirmation flow. No authenticated session is required. */
public final class EmailConfirmationService {
    private final ActionTokenDAO tokenDAO = new ActionTokenDAO();

    public boolean isConfirmable(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 64) return false;
        return tokenDAO.findValidToken(TokenUtil.hashToken(rawToken)) != null;
    }

    public boolean confirm(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.length() < 40 || rawToken.length() > 64) return false;
        return tokenDAO.confirmAndScheduleNext(TokenUtil.hashToken(rawToken));
    }
}
