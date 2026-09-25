package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestQuery;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.ApproveAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.RejectAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SearchAccessRequestsUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SubmitAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto.AccessRequestPageResponse;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto.AccessRequestResponse;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto.RejectAccessRequestRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto.SubmitAccessRequestRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto.SubmitAccessRequestResponse;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driving Adapter (Presentation). Exposes REST endpoints.
 */
@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
@Tag(name = "Access requests", description = "Solicitação pública e decisão administrativa de acesso")
public class AccessRequestController {

    private final SubmitAccessRequestUseCase submitAccessRequestUseCase;
    private final SearchAccessRequestsUseCase searchAccessRequestsUseCase;
    private final ApproveAccessRequestUseCase approveAccessRequestUseCase;
    private final RejectAccessRequestUseCase rejectAccessRequestUseCase;

    @PostMapping
    @Operation(summary = "Solicitar acesso", description = "Registra uma solicitação pública pendente de aprovação")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitação recebida ou já pendente"),
            @ApiResponse(responseCode = "409", description = "E-mail indisponível para nova solicitação",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Limite de solicitações excedido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SubmitAccessRequestResponse> submit(
            @RequestBody @Valid SubmitAccessRequestRequest request
    ) {
        submitAccessRequestUseCase.execute(AccessRequest.builder()
                .name(request.name())
                .email(request.email())
                .requestReason(request.reason())
                .build());
        return ResponseEntity.accepted().body(
                new SubmitAccessRequestResponse("Solicitação recebida para análise")
        );
    }

    @GetMapping
    @Operation(summary = "Listar solicitações", description = "Lista solicitações paginadas. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public AccessRequestPageResponse search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccessRequestStatus status
    ) {
        return AccessRequestPageResponse.from(searchAccessRequestsUseCase.execute(
                new AccessRequestQuery(page, size, search, status)
        ));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprovar solicitação",
            description = "Cria uma conta STUDENT inativa e envia o convite uma única vez. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação aprovada"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Solicitação já decidida ou e-mail cadastrado")
    })
    public AccessRequestResponse approve(
            @PathVariable String id,
            @AuthenticationPrincipal User administrator
    ) {
        return AccessRequestResponse.from(
                approveAccessRequestUseCase.execute(id, administrator.getId())
        );
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeitar solicitação",
            description = "Registra uma decisão definitiva com justificativa. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public AccessRequestResponse reject(
            @PathVariable String id,
            @AuthenticationPrincipal User administrator,
            @RequestBody @Valid RejectAccessRequestRequest request
    ) {
        return AccessRequestResponse.from(
                rejectAccessRequestUseCase.execute(id, administrator.getId(), request.reason())
        );
    }
}
