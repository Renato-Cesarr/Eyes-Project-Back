package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;

public interface SearchUsersUseCase {
    PageResult<UserView> execute(UserQuery query);
}
