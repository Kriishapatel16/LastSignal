package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import model.CheckInSettingsBean;
import service.CheckInSettingsService;

@WebServlet("/SettingsServlet")
public class SettingsServlet extends HttpServlet {
    private final CheckInSettingsService service = new CheckInSettingsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        Integer userId = authenticatedUserId(request);
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            request.setAttribute("settings", service.getSettings(userId));
            request.getRequestDispatcher("/settings.jsp").forward(request, response);
        } catch (Exception ex) {
            getServletContext().log("Unable to load check-in settings for user " + userId, ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load settings. Please try again.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        Integer userId = authenticatedUserId(request);
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!validCsrf(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean saved = false;
        try {
            CheckInSettingsBean settings = new CheckInSettingsBean();
            settings.setUserId(userId); // Never accept the account ID from the form.
            settings.setIntervalHours(parseRequiredInt(request, "intervalHours"));
            settings.setGraceMinutes(parseRequiredInt(request, "graceMinutes"));
            settings.setEscalationDelay(parseRequiredInt(request, "escalationDelay"));
            settings.setEnabled("true".equals(request.getParameter("monitoringEnabled")));

            service.saveSettings(settings);
            request.getSession(false).setAttribute("settingsMessage", "Settings saved successfully. Your next check-in schedule has been reset.");
            saved = true;
        } catch (IllegalArgumentException ex) {
            request.getSession(false).setAttribute("settingsError", ex.getMessage());
        } catch (Exception ex) {
            getServletContext().log("Unable to save check-in settings for user " + userId, ex);
            request.getSession(false).setAttribute("settingsError",
                    "Settings could not be saved. Please try again.");
        }
        response.sendRedirect(request.getContextPath()
                + (saved ? "/DashboardServlet" : "/SettingsServlet"));
    }

    private int parseRequiredInt(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty() || value.length() > 4) {
            throw new IllegalArgumentException("Missing setting.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid setting.");
        }
    }

    private Integer authenticatedUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Integer)) {
            return null;
        }
        Integer userId = (Integer) session.getAttribute("userId");
        return userId > 0 ? userId : null;
    }

    private boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object token = session.getAttribute("csrfToken");
        String supplied = request.getParameter("csrfToken");
        if (!(token instanceof String) || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(((String) token).getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
