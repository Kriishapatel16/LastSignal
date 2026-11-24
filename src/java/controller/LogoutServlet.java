
package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import util.CsrfUtil;

@WebServlet("/logout")
public final class LogoutServlet extends HttpServlet {

    private void preventCaching(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        preventCaching(response);

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        if (!CsrfUtil.isValid(request)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Invalid request. Reload the page and try again."
            );
            return;
        }

        session.invalidate();

        response.sendRedirect(
                request.getContextPath() + "/login.jsp"
        );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        preventCaching(response);

        response.sendError(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED
        );
    }
}
