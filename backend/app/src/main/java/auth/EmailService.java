package auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender; // may be null if mail not configured

    @Value("${app.mail.from:noreply@localhost}")
    private String fromAddress;

    public EmailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordReset(String to, String resetLink) {
        String subject = "Password reset instructions";
        String text = "We received a request to reset your password.\n\n" +
                "To reset it, click the link below (or copy and paste it into your browser):\n" +
                resetLink + "\n\n" +
                "If you did not request a password reset, you can ignore this email.";

        if (mailSender == null) {
            // No-op fallback: log the email so we don't break flows in non-mail environments
            log.warn("JavaMailSender not configured. Would have sent password reset email to {} with link {}", to, resetLink);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Password reset email sent to {}", to);
        } catch (MailException ex) {
            log.warn("Failed to send password reset email to {}: {}", to, ex.toString());
        }
    }
}
