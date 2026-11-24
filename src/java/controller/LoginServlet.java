
package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.UserBean;
import service.UserService;
import util.AuthRateLimiter;
import util.CsrfUtil;

@WebServlet("/login")
public final class LoginServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(LoginServlet.class.getName());

    private final UserService userService = new UserService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(
                request.getContextPath() + "/login.jsp"
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (!CsrfUtil.isValid(request)) {

            CsrfUtil.rotateToken(request.getSession(true));

            request.setAttribute(
                    "error",
                    "Your form expired. Please try again."
            );

            request.getRequestDispatcher("/login.jsp")
                    .forward(request, response);

            return;
        }

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        String normalizedEmail = email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.isEmpty()
                || normalizedEmail.length() > 254
                || password == null
                || password.isEmpty()
                || password.length() > 128) {

            request.setAttribute(
                    "error",
                    "Enter a valid email address and password."
            );

            request.getRequestDispatcher("/login.jsp")
                    .forward(request, response);

            return;
        }

        String remoteAddress = request.getRemoteAddr();

        String emailKey = "email:" + normalizedEmail;

        String ipKey = "ip:"
                + (remoteAddress == null ? "unknown" : remoteAddress);

        if (AuthRateLimiter.isBlocked(emailKey)
                || AuthRateLimiter.isBlocked(ipKey)) {

            response.setStatus(429); // HTTP 429 - Too Many Requests

            request.setAttribute(
                    "error",
                    "Too many unsuccessful attempts. Please wait 15 minutes before trying again."
            );

            request.getRequestDispatcher("/login.jsp")
                    .forward(request, response);

            return;
        }

        final UserBean user;

        try {

            user = userService.login(
                    normalizedEmail,
                    password
            );

        } catch (IllegalStateException exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Login database operation failed.",
                    exception
            );

            response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );

            request.setAttribute(
                    "error",
                    "Login is temporarily unavailable. Please try again later."
            );

            request.getRequestDispatcher("/login.jsp")
                    .forward(request, response);

            return;
        }

        if (user == null) {

            AuthRateLimiter.recordFailure(emailKey);
            AuthRateLimiter.recordFailure(ipKey);

            request.setAttribute(
                    "error",
                    "Invalid email or password."
            );

            request.getRequestDispatcher("/login.jsp")
                    .forward(request, response);

            return;
        }

        AuthRateLimiter.recordSuccess(emailKey);
        AuthRateLimiter.recordSuccess(ipKey);

        // Invalidate the anonymous session to prevent session fixation.
        HttpSession previousSession = request.getSession(false);

        if (previousSession != null) {
            previousSession.invalidate();
        }

        // Create a fresh authenticated session.
        HttpSession authenticatedSession =
                request.getSession(true);

        authenticatedSession.setAttribute(
                "userId",
                user.getUserId()
        );

        authenticatedSession.setAttribute(
                "userName",
                user.getName()
        );

        authenticatedSession.setAttribute(
                "userRole",
                user.getRole()
        );

        CsrfUtil.rotateToken(authenticatedSession);

        authenticatedSession.setMaxInactiveInterval(30 * 60);

        response.sendRedirect(
                request.getContextPath() + "/DashboardServlet"
        );
    }
}
