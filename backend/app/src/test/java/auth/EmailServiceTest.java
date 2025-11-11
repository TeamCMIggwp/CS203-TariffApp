package auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@example.com");
    }

    @Test
    void sendPasswordReset_withConfiguredMailSender_sendsEmail() {
        // Arrange
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:3000/reset-password?token=abc123";

        // Act
        emailService.sendPasswordReset(toEmail, resetLink);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("test@example.com");
        assertThat(sentMessage.getTo()).containsExactly(toEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("Password reset instructions");
        assertThat(sentMessage.getText()).contains(resetLink);
        assertThat(sentMessage.getText()).contains("reset your password");
    }

    @Test
    void sendPasswordReset_withNullMailSender_doesNotThrow() {
        // Arrange
        EmailService emailServiceWithoutSender = new EmailService(null);
        ReflectionTestUtils.setField(emailServiceWithoutSender, "fromAddress", "test@example.com");
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:3000/reset-password?token=abc123";

        // Act - should not throw
        emailServiceWithoutSender.sendPasswordReset(toEmail, resetLink);

        // Assert - no exception thrown
    }

    @Test
    void sendPasswordReset_withMailException_doesNotThrow() {
        // Arrange
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:3000/reset-password?token=abc123";

        doThrow(new MailException("SMTP server unavailable") {})
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act - should not throw (exception is caught and logged)
        emailService.sendPasswordReset(toEmail, resetLink);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordReset_containsProperEmailContent() {
        // Arrange
        String toEmail = "recipient@example.com";
        String resetLink = "http://frontend.com/reset?token=xyz789";

        // Act
        emailService.sendPasswordReset(toEmail, resetLink);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String text = sentMessage.getText();

        assertThat(text).contains("We received a request to reset your password");
        assertThat(text).contains(resetLink);
        assertThat(text).contains("If you did not request a password reset");
    }

    @Test
    void sendPasswordReset_setsCorrectFromAddress() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "fromAddress", "custom@company.com");
        String toEmail = "user@example.com";
        String resetLink = "http://localhost:3000/reset";

        // Act
        emailService.sendPasswordReset(toEmail, resetLink);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("custom@company.com");
    }
}
