package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Compatibility endpoint for bookmarks or stale forms from the former
 * dashboard-based confirmation flow. It never confirms a check-in.
 * The only confirmation action is the one-time token flow in
 * EmailConfirmationServlet.
 */
@WebServlet("/CheckInServlet")
public final class CheckInServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        redirectToCurrentFlow(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        redirectToCurrentFlow(request, response);
    }

    private void redirectToCurrentFlow(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");

        if (userId instanceof Integer && ((Integer) userId) > 0) {
            session.setAttribute("checkinMessage",
                    "Safety confirmation is now completed from the secure link in your reminder email. "
                    + "Open that email and press Confirm I'm Safe. No dashboard confirmation is needed.");
            response.sendRedirect(request.getContextPath() + "/DashboardServlet");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
}
