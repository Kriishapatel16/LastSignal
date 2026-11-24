package filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.UserBean;
import service.UserService;

/** Default-deny page filter: only explicit public routes bypass authentication. */
@WebFilter(urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST})
public final class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getServletPath();

        // Data-backed JSPs must be reached through their controller servlets.
        if ("/dashboard.jsp".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/DashboardServlet");
            return;
        }
        if ("/settings.jsp".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/SettingsServlet");
            return;
        }
        if ("/contacts.jsp".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/contacts");
            return;
        }
        if ("/checkin.jsp".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/DashboardServlet");
            return;
        }

        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setDateHeader("Expires", 0);
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Referrer-Policy", "same-origin");
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");
        if (!(userId instanceof Integer) || ((Integer) userId) <= 0) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp");
            return;
        }

        // Re-check account state so an administrator's deactivation takes effect
        // on the next protected request instead of leaving an old session active.
        try {
            UserBean account = new UserService().getUserById((Integer) userId);
            if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                if (session != null) session.invalidate();
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp");
                return;
            }
        } catch (IllegalStateException ex) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Account status could not be verified. Please try again.");
            return;
        }

        if (isAdminPath(path)) {
            Object role = session.getAttribute("userRole");
            if (!(role instanceof String) || !"ADMIN".equalsIgnoreCase((String) role)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)
                || "/index.html".equals(path)
                || "/login.jsp".equals(path)
                || "/register.jsp".equals(path)
                || "/login".equals(path)
                || "/register".equals(path)
                || "/EmailConfirmationServlet".equals(path)
                || "/favicon.ico".equals(path)) {
            return true;
        }
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/fonts/")
                || path.startsWith("/jakarta.faces.resource/");
    }

    private boolean isAdminPath(String path) {
        return "/admin.xhtml".equals(path)
                || "/users.xhtml".equals(path)
                || "/notifications.xhtml".equals(path);
    }

    @Override
    public void destroy() { }
}
