package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import service.EmailConfirmationService;

/** Public confirmation endpoint. GET only previews; POST performs the one-time action. */
@WebServlet(name = "EmailConfirmationServlet", urlPatterns = {"/EmailConfirmationServlet"})
public final class EmailConfirmationServlet extends HttpServlet {
    private final EmailConfirmationService service = new EmailConfirmationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        secureHeaders(response);
        String token = request.getParameter("token");
        try {
            if (!service.isConfirmable(token)) {
                render(response, HttpServletResponse.SC_GONE, "Link expired or unavailable",
                        "This confirmation link is invalid, expired, already used, or monitoring has been disabled.", null);
                return;
            }
            render(response, HttpServletResponse.SC_OK, "Confirm your safety",
                    "Press the button below to confirm that you are safe. This action does not require you to log in.", token);
        } catch (Exception ex) {
            getServletContext().log("Unable to validate an email check-in link.", ex);
            render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Temporarily unavailable",
                    "We could not verify this link right now. Please try again shortly.", null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        secureHeaders(response);
        request.setCharacterEncoding("UTF-8");
        String token = request.getParameter("token");
        try {
            if (service.confirm(token)) {
                render(response, HttpServletResponse.SC_OK, "You're confirmed safe",
                        "Your safety check-in has been recorded. Your next check-in has been scheduled. No trusted-contact alert will be sent for this check-in.", null);
            } else {
                render(response, HttpServletResponse.SC_GONE, "Link expired or unavailable",
                        "This link can no longer confirm the check-in. It may have expired, already been used, or monitoring may have been disabled.", null);
            }
        } catch (Exception ex) {
            getServletContext().log("Email-based safety confirmation failed.", ex);
            render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Temporarily unavailable",
                    "Your check-in could not be recorded right now. Please try again shortly.", null);
        }
    }

    private void secureHeaders(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
    }

    private void render(HttpServletResponse response, int status, String heading,
            String message, String token) throws IOException {
        response.setStatus(status);
        StringBuilder html = new StringBuilder(1400);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>LastSignal | ").append(escape(heading)).append("</title>")
                .append("<style>body{margin:0;background:#f6f3ed;color:#292923;font:16px Arial,sans-serif;display:grid;min-height:100vh;place-items:center}.card{box-sizing:border-box;width:min(92%,480px);background:#fff;padding:36px;border:1px solid #e8e1d6;border-radius:16px;box-shadow:0 12px 36px #29292312}h1{font-size:26px;margin:0 0 14px}p{line-height:1.65;color:#625f57}.btn{display:inline-block;border:0;border-radius:8px;background:#59634b;color:white;padding:14px 22px;font-size:16px;font-weight:600;cursor:pointer;margin-top:12px}.brand{font-size:13px;letter-spacing:2px;color:#777;margin-bottom:22px}</style>")
                .append("</head><body><main class=\"card\"><div class=\"brand\">LASTSIGNAL</div><h1>")
                .append(escape(heading)).append("</h1><p>").append(escape(message)).append("</p>");
        if (token != null) {
            html.append("<form method=\"post\" action=\"")
                    .append(escape(getServletContext().getContextPath()))
                    .append("/EmailConfirmationServlet\"><input type=\"hidden\" name=\"token\" value=\"")
                    .append(escape(token)).append("\"><button class=\"btn\" type=\"submit\">Confirm I'm Safe</button></form>");
        }
        html.append("</main></body></html>");
        response.getWriter().write(html.toString());
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
