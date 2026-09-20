package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SearchUsersUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchUsersUseCaseImpl implements SearchUsersUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserView> execute(UserQuery query) {
        return userRepository.search(query).map(UserView::from);
    }
}
