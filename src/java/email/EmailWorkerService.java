package email;

/** Central mail gateway; isolates SMTP transport from scheduling/business logic. */
public final class EmailWorkerService {
    private final EmailService emailService = new EmailService();

    public void send(String recipient, String subject, String body) throws Exception {
        emailService.send(recipient, subject, body);
    }

    public void sendHtml(String recipient, String subject, String plainText, String htmlBody)
            throws Exception {
        emailService.sendHtml(recipient, subject, plainText, htmlBody);
    }
}
