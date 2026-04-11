package br.com.eyesproject.eyes_project_back.modules.user.application.ports.out;

public interface EmailSenderPort {
    void sendInvitationEmail(String toEmail, String userName, String setupToken);
    void sendPasswordResetEmail(String toEmail, String userName, String resetToken);
}
