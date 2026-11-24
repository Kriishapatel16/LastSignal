package email;

import java.sql.Timestamp;

public final class EmailTemplates {
    private EmailTemplates() { }

    public static String checkInReminder(String name, Timestamp dueAt, String confirmationUrl) {
        return "Hello " + safe(name) + ",\n\n"
                + "Your LastSignal safety check-in is due.\n"
                + "If you are safe, open the link below and press 'Confirm I'm Safe'. "
                + "You do not need to log in.\n\n"
                + confirmationUrl + "\n\n"
                + "This confirmation link expires at the end of your configured grace period "
                + "and can be used only once.\n"
                + "Scheduled time: " + dueAt + "\n\n"
                + "If you do not confirm before the grace period ends, LastSignal will email "
                + "your active trusted contacts in priority order.\n\n"
                + "If you did not request this check-in, you can ignore this email.\n"
                + "LastSignal is an automated notification service; it does not detect emergencies.\n";
    }

    public static String checkInReminderHtml(String name, Timestamp dueAt, String confirmationUrl) {
        return "<!doctype html><html lang=\"en\"><body style=\"margin:0;padding:32px;background:#f6f3ed;font-family:Arial,sans-serif;color:#292923\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" style=\"max-width:560px;background:#ffffff;border:1px solid #e8e1d6;border-radius:14px\" cellspacing=\"0\" cellpadding=\"28\"><tr><td>"
                + "<div style=\"font-size:12px;letter-spacing:2px;color:#777;margin-bottom:22px\">LASTSIGNAL</div>"
                + "<h1 style=\"font-size:25px;margin:0 0 16px\">Your safety check-in is due</h1>"
                + "<p style=\"line-height:1.6;color:#625f57\">Hello " + html(name)
                + ", please confirm that you are safe. You do not need to log in.</p>"
                + "<p style=\"line-height:1.6;color:#625f57\">Scheduled time: " + html(String.valueOf(dueAt)) + "</p>"
                + "<p style=\"margin:28px 0\"><a href=\"" + html(confirmationUrl)
                + "\" style=\"display:inline-block;background:#59634b;color:#ffffff;text-decoration:none;padding:14px 22px;border-radius:8px;font-weight:bold\">Confirm I'm Safe</a></p>"
                + "<p style=\"font-size:13px;line-height:1.6;color:#777\">This one-time link expires at the end of your grace period. If you do not confirm in time, your active trusted contacts will be notified in priority order.</p>"
                + "<p style=\"font-size:12px;line-height:1.6;color:#888\">LastSignal is an automated notification service and does not detect emergencies.</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static String html(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String trustedContactAlert(String contactName, String userName,
            Timestamp scheduledAt, int priority) {
        return "Hello " + safe(contactName) + ",\n\n"
                + "MISSED SAFETY CHECK-IN ALERT\n\n"
                + "The registered LastSignal user, " + safe(userName)
                + ", has NOT confirmed that they are safe for the scheduled check-in.\n\n"
                + "Scheduled check-in: " + scheduledAt + "\n"
                + "Your notification priority: " + priority + "\n\n"
                + "Please try to contact them using the contact details you already have. "
                + "If you believe there is an immediate emergency, contact local emergency services.\n\n"
                + "LastSignal is an automated notification service. It does not verify emergencies "
                + "or dispatch emergency responders.\n";
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "there" : value.trim();
    }
}
