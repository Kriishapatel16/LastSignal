package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.UserBean;
import service.UserService;
import util.CsrfUtil;

@WebServlet("/register")
public final class RegisterServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (!CsrfUtil.isValid(request)) {
            CsrfUtil.rotateToken(request.getSession(true));
            request.setAttribute("error", "Your form expired. Please try again.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        String password = request.getParameter("password");
        String confirmation = request.getParameter("confirmPassword");
        if (password == null || !password.equals(confirmation)) {
            request.setAttribute("error", "Passwords do not match.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        String email = request.getParameter("email");
        String result = userService.register(request.getParameter("name"),
                email, password, request.getParameter("phone"));

        if (!result.startsWith("Registration successful.")) {
            request.setAttribute("error", result);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // Authenticate the newly-created ACTIVE account immediately.
        UserBean user;
        try {
            user = userService.login(email, password);
        } catch (IllegalStateException ex) {
            request.getSession(false).setAttribute("registrationMessage",
                    "Your account was created. Please log in to continue.");
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        if (user == null) {
            request.getSession(false).setAttribute("registrationMessage",
                    "Your account was created. Please log in to continue.");
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("registrationMessage",
                "Your account has been created successfully. Welcome to LastSignal.");
        CsrfUtil.rotateToken(session);
        session.setMaxInactiveInterval(30 * 60);

        response.sendRedirect(request.getContextPath() + "/DashboardServlet");
    }
}
