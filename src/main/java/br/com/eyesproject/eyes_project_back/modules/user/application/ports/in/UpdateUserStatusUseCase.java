package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;

public interface UpdateUserStatusUseCase {
    UserView execute(String id, boolean active);
}
