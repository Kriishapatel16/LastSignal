
package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

import model.CheckInBean;
import model.CheckInSettingsBean;
import service.CheckInService;
import service.CheckInSettingsService;

@WebServlet("/DashboardServlet")
public class DashboardServlet extends HttpServlet {

    private final CheckInService checkInService = new CheckInService();
    private final CheckInSettingsService settingsService = new CheckInSettingsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession session = request.getSession(false);

        if (session == null
                || !(session.getAttribute("userId") instanceof Integer)) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        if (userId <= 0) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        try {
            CheckInSettingsBean settings =
                    settingsService.getSettings(userId);

            CheckInBean currentCheckIn =
                    checkInService.getCurrentCheckIn(userId);

            CheckInBean latestCheckIn =
                    checkInService.getLatestCheckIn(userId);

            request.setAttribute("settings", settings);
            request.setAttribute("currentCheckIn", currentCheckIn);
            request.setAttribute("latestCheckIn", latestCheckIn);

            request.getRequestDispatcher("/dashboard.jsp")
                    .forward(request, response);

        } catch (SQLException ex) {
            getServletContext().log(
                    "Unable to load dashboard for user ID: " + userId, ex);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load your dashboard. Please try again later.");

        } catch (IllegalArgumentException ex) {
            getServletContext().log(
                    "Invalid dashboard request for user ID: " + userId, ex);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
