package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;

public interface GetUserUseCase {
    UserView execute(String id);
}
