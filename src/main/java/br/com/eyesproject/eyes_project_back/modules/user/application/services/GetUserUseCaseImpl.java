package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.GetUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserUseCaseImpl implements GetUserUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserView execute(String id) {
        return userRepository.findById(id)
                .map(UserView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
