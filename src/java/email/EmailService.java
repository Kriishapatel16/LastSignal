package email;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** SMTP transport configured only through process environment variables. */
public final class EmailService {

    public void send(String recipient, String subject, String body) throws Exception {
        MimeMessage message = newMessage(recipient, subject);
        message.setText(body, StandardCharsets.UTF_8.name());
        Transport.send(message);
    }

    public void sendHtml(String recipient, String subject, String plainText, String htmlBody)
            throws Exception {
        MimeMessage message = newMessage(recipient, subject);
        MimeMultipart multipart = new MimeMultipart("alternative");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(plainText, StandardCharsets.UTF_8.name());
        multipart.addBodyPart(textPart);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);
        Transport.send(message);
    }

    private MimeMessage newMessage(String recipient, String subject) throws Exception {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }

        String host = required("LASTSIGNAL_SMTP_HOST");
        String username = required("LASTSIGNAL_SMTP_USERNAME");
        String password = required("LASTSIGNAL_SMTP_PASSWORD");
        String from = value("LASTSIGNAL_SMTP_FROM", username);
        String port = value("LASTSIGNAL_SMTP_PORT", "587");
        boolean ssl = Boolean.parseBoolean(value("LASTSIGNAL_SMTP_SSL", "false"));
        boolean startTls = Boolean.parseBoolean(value("LASTSIGNAL_SMTP_STARTTLS", "true"));

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "15000");
        properties.put("mail.smtp.writetimeout", "15000");
        properties.put("mail.smtp.starttls.required", Boolean.toString(startTls));

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        return message;
    }

    private static String required(String name) {
        String result = System.getenv(name);
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("Missing SMTP configuration: " + name);
        }
        return result.trim();
    }

    private static String value(String name, String fallback) {
        String result = System.getenv(name);
        return result == null || result.isBlank() ? fallback : result.trim();
    }
}
