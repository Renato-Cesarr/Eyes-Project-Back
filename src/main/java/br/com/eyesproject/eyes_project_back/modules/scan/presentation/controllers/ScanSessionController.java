package br.com.eyesproject.eyes_project_back.modules.scan.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.scan.application.services.ScanSessionService;
import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanAggregate;
import br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto.*;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scan-sessions")
@RequiredArgsConstructor
@Tag(name = "Scan sessions", description = "Metadados consentidos; nenhum frame, imagem ou áudio")
@SecurityRequirement(name = "bearerAuth")
public class ScanSessionController {
    private final ScanSessionService service;

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Registrar início de sessão", description = "Consentimento metadata-sync-v1 obrigatório; retry idêntico retorna a mesma sessão.")
    public ScanSessionResponse start(@AuthenticationPrincipal User user, @Valid @RequestBody StartScanRequest request) {
        return ScanSessionResponse.from(service.start(UUID.fromString(user.getId()), request.toDomain()));
    }
    @PostMapping(value = "/{id}/events", consumes = "application/json")
    @Operation(summary = "Enviar até 50 eventos anunciados", description = "Lote atômico, máximo 200 por sessão; IDs estáveis evitam duplicação.")
    public BatchReceipt append(@AuthenticationPrincipal User user, @PathVariable UUID id,
                               @Valid @RequestBody DetectionBatchRequest request) {
        int total = service.append(UUID.fromString(user.getId()), id,
                request.events().stream().map(DetectionRequest::toDomain).toList());
        return new BatchReceipt(id, total);
    }
    @PostMapping(value = "/{id}/finish", consumes = "application/json")
    @Operation(summary = "Finalizar sessão e métricas agregadas", description = "Enviar após todos os lotes; retry idêntico é aceito.")
    public ScanSessionResponse finish(@AuthenticationPrincipal User user, @PathVariable UUID id,
                                     @Valid @RequestBody FinishScanRequest request) {
        return ScanSessionResponse.from(service.finish(UUID.fromString(user.getId()), id,
                request.endedAt(), request.metrics().toDomain()));
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar própria sessão", description = "Sessão alheia, inexistente ou expirada retorna 404, inclusive para ADMIN.")
    public ScanSessionResponse get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return ScanSessionResponse.from(service.get(UUID.fromString(user.getId()), id));
    }
    @DeleteMapping
    @Operation(summary = "Excluir o próprio histórico", description = "Exclusão física de sessões e eventos em cascata, inclusive com coleta desabilitada.")
    public ResponseEntity<Void> deleteHistory(@AuthenticationPrincipal User user) {
        service.deleteHistory(UUID.fromString(user.getId()));
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/aggregate")
    @Operation(summary = "Consultar totais sem identidade", description = "Somente ADMIN. Totais operacionais reportados pelo cliente não comprovam avaliação científica.")
    public ScanAggregate aggregate() { return service.aggregate(); }

    public record BatchReceipt(UUID sessionId, int storedEvents) {}
}
