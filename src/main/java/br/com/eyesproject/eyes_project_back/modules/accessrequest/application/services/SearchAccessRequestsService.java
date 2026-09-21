package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestPage;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestQuery;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SearchAccessRequestsUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchAccessRequestsService implements SearchAccessRequestsUseCase {

    private final AccessRequestRepository accessRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public AccessRequestPage execute(AccessRequestQuery query) {
        return accessRequestRepository.search(query);
    }
}
