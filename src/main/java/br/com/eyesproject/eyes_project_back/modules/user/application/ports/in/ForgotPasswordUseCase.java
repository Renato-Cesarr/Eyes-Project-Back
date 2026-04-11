package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

public interface ForgotPasswordUseCase {
    void execute(String email);
}
