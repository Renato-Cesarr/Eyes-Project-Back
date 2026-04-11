package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailAdapter implements EmailSenderPort {

    private final JavaMailSender javaMailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void sendInvitationEmail(String toEmail, String userName, String setupToken) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Bem-vindo ao Eyes Project - Defina sua senha");
            helper.setFrom("no-reply@eyesproject.com");

            String setupLink = frontendUrl + "/setup-password?token=" + setupToken;

            String htmlMsg = "<h3>Olá, " + userName + "!</h3>"
                    + "<p>Você foi convidado para acessar o painel do Eyes Project.</p>"
                    + "<p>Para ativar sua conta e gerenciar seus acessos, defina sua senha clicando no link abaixo:</p>"
                    + "<a href=\"" + setupLink + "\"><strong>Definir minha senha</strong></a>"
                    + "<br><br><p>Se você não solicitou isso, ignore este e-mail.</p>";

            helper.setText(htmlMsg, true);

            javaMailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Recuperação de Senha - Eyes Project");
            helper.setFrom("no-reply@eyesproject.com");

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            String htmlMsg = "<h3>Olá, " + userName + "!</h3>"
                    + "<p>Recebemos uma solicitação para redefinir a sua senha no Eyes Project.</p>"
                    + "<p>Para criar uma nova senha, clique no link abaixo:</p>"
                    + "<a href=\"" + resetLink + "\"><strong>Redefinir minha senha</strong></a>"
                    + "<br><br><p>Se você não solicitou isso, ignore este e-mail.</p>";

            helper.setText(htmlMsg, true);

            javaMailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
