package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

public interface ResetPasswordUseCase {
    void execute(String token, String newPassword);
}
