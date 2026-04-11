package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailAdapterTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private SmtpEmailAdapter smtpEmailAdapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smtpEmailAdapter, "frontendUrl", "http://test-front.com");
    }

    @Test
    @DisplayName("Should create and send invitation email successfully")
    void shouldSendInvitationEmail() {
        // Arrange
        String email = "user@test.com";
        String name = "User Name";
        String token = "setup-token-123";
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        smtpEmailAdapter.sendInvitationEmail(email, name, token);

        // Assert
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should create and send password reset email successfully")
    void shouldSendPasswordResetEmail() {
        // Arrange
        String email = "user@test.com";
        String name = "User Name";
        String token = "reset-token-123";
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        smtpEmailAdapter.sendPasswordResetEmail(email, name, token);

        // Assert
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}
