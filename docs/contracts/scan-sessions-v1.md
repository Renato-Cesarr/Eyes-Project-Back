# Sessões remotas consentidas — contrato v1

Implementação da REN-18 para o consumidor REN-75. Este documento não declara upload mobile, validação física ou conclusão da REN-73.

## Escopo e privacidade

Assistência, câmera, inferência, proximidade, TTS e háptico continuam locais e independentes da API. Conta e coleta são opcionais. O servidor inicia com `SCAN_COLLECTION_ENABLED=false`; habilitar somente no ambiente destinado a receber metadados consentidos. O cliente deve coletar/enfileirar somente depois do consentimento explícito, vincular a fila à conta que consentiu e enviar após encerrar a sessão local.

JSON possui campos tipados, sem mapa arbitrário de propriedades. Campos desconhecidos são rejeitados em todos os níveis. Não há campos para imagem, frame, vídeo, áudio, bounding box, OCR, GPS ou dados de hardware. Identificação de modelo aceita apenas letras ASCII, números, ponto, hífen e underscore, até 80 caracteres. Não existe BLOB, mídia ou JSONB livre no schema.

`installationId` é UUID aleatório e renovável da instalação. Não usar IMEI, serial, MAC ou fingerprint. Os dados são **pseudônimos, não anônimos**: `owner_id` vincula registros à conta para autorização e exclusão. Métricas reportadas pelo cliente não comprovam avaliação científica independente.

## Transporte e autorização

Base: `/api/v1/scan-sessions`. JWT no header Authorization Bearer, usuário ativo, papel STUDENT ou ADMIN. O proprietário vem da autenticação, nunca do body. Login existente retorna `token`; `accessToken` é a representação interna do mobile.

POST requer `application/json`. Body máximo: **65.536 bytes**, inclusive transferência sem Content-Length. Não há multipart/base64/mídia. Instants em ISO-8601 com offset/UTC são normalizados em milissegundos para retries e armazenados em TIMESTAMPTZ. IDs do cliente devem ser UUIDs estáveis.

| Método e rota relativa à base | Resposta | Comportamento |
| --- | --- | --- |
| POST / | 200 + sessão | Cria ou retorna sessão idêntica |
| POST /{id}/events | 200 + sessionId/storedEvents | Lote atômico; total inclui eventos anteriores |
| POST /{id}/finish | 200 + sessão | Finaliza depois de todos os lotes |
| GET /{id} | 200 + sessão | Somente proprietário |
| DELETE / | 204 | Exclui próprio histórico e eventos em cascata |
| GET /aggregate | 200 + totais | Somente ADMIN, sem identidades/eventos individuais |

POST/DELETE da raiz usam exatamente a base, sem barra final obrigatória. Sessão alheia, inexistente ou expirada retorna 404, inclusive para ADMIN. ADMIN acessa somente seu próprio histórico individual e o agregado geral. Leitura/exclusão continuam disponíveis com coleta desabilitada.

## Início e consentimento

Exemplo ilustrativo; adaptar IDs/horários à sessão real:

```json
{
  "schemaVersion": 1,
  "clientSessionId": "eb98867b-a63c-4d20-bff3-8cd405f1d8fe",
  "installationId": "e76c6742-53cf-41fb-9e3c-d6dca3f240d9",
  "startedAt": "2026-10-08T20:00:00.000Z",
  "modelId": "efficientdet-lite0-coco2017-int8",
  "modelVersion": "tensorflow-metadata-1",
  "consentGranted": true,
  "consentVersion": "metadata-sync-v1"
}
```

Versão 1 e consentVersion metadata-sync-v1 são obrigatórios; consentGranted false/ausente é rejeitado. consent_received_at registra a chegada da declaração, **não comprova consentimento anterior à coleta local**. REN-75 deve provar essa sequência. Início nos últimos 30 dias, tolerância futura de cinco minutos; duração máxima de duas horas. O cliente deve segmentar sessões mais longas.

Resposta: id remoto, clientSessionId, startedAt, endedAt, expiresAt, modelId, modelVersion e metrics. Antes da finalização, fim/métricas são nulos. A primeira criação e o retry retornam 200. Chave única por proprietário; reutilizar com metadados diferentes retorna 409. Lock do proprietário serializa criação concorrente e quota.

## Eventos anunciados

```json
{
  "events": [{
    "clientEventId": "920416aa-b21c-4dd5-9cde-844730f21389",
    "objectClass": "table_desk",
    "confidence": 0.812345,
    "proximityBand": "veryNear",
    "direction": "ahead",
    "occurredAt": "2026-10-08T20:00:05.000Z"
  }]
}
```

Classes: person, chair, table_desk, backpack, conforme domain_id do manifesto. Mobile DetectedObjectKind.table mapeia para table_desk. Faixas: distant, attention, veryNear; direções: left, ahead, right. Proximidade é relativa, sem metros. Confiança em [0,1], até seis casas decimais: arredondar uma vez antes de enfileirar e manter no retry.

Enviar somente eventos que geraram aviso, nunca todas as detecções de frames. Limites: 1–50 eventos por lote, **200 por sessão**. Consumidor deve segmentar/indicar limite conforme REN-75, sem descartar excesso silenciosamente ou alegar sincronização completa. Evento estabilizado não comprova áudio entregue.

Lock de sessão serializa append/finish. Chave de evento única por sessão: conteúdo idêntico não duplica; conteúdo diferente retorna 409 e faz rollback de todo o lote. Timestamp deve cair entre início e início + duas horas e respeitar tolerância de relógio. Sessão finalizada aceita retry de eventos idênticos existentes, nunca evento novo.

## Finalização e agregados

```json
{
  "endedAt": "2026-10-08T20:01:00.000Z",
  "metrics": {
    "processedFrames": 300,
    "inferenceMillisTotal": 12000,
    "ttsLatencySamples": 1,
    "ttsLatencyMillisTotal": 700
  }
}
```

Frames: 0–1.000.000; inferenceMillisTotal e ttsLatencyMillisTotal: 0–1.000.000.000; samples TTS: 0–200. Somas inteiras em milissegundos não substituem p50/p95, métricas científicas, sustentação ou REN-37/69. REN-75 deve usar landmarks realmente observados do protocolo REN-68, sem fabricar dados ausentes.

Frames não podem ser menores que eventos; samples TTS não podem superar eventos. Zero frames/samples exige soma correspondente zero. Fim inclui todos os eventos e respeita intervalo de sessão/tolerância futura. Finalização idêntica é idempotente; alterar fim/métricas depois retorna 409.

Agregado ADMIN inclui sessions, completedSessions, announcedEvents, processedFrames, inferenceMillisTotal, ttsLatencySamples e ttsLatencyMillisTotal. Não inclui userId, instalação, sessão individual ou lista de eventos.

## Retenção, exclusão e operação

Retenção: **30 × 24 horas a partir do início**. expiresAt explícito; retry não renova prazo. Máximo 1.000 sessões não expiradas por conta. Dados expirados ficam imediatamente indisponíveis para leitura, escrita e agregado.

`SCAN_COLLECTION_CLEANUP_ENABLED=true` por padrão. Job inicia um minuto após boot e executa a cada hora, removendo até 1.000 sessões expiradas por transação com índice/SKIP LOCKED e exclusão em cascata. Pode existir backlog de purga física; monitorar expiradas antes de aumentar volume. Expiração não promete exclusão imediata de bytes de backups. Não existe política de backup/implantação comprovada neste incremento. Testes executam a operação de purga explicitamente; não observam uma hora de agendamento.

DELETE remove todo o histórico da conta atual, inclusive expiradas, sem afetar outros usuários/dados administrativos. Cliente deve pausar envio, limpar/inutilizar fila e cancelar retries ao revogar consentimento, sair da conta ou solicitar exclusão. **A API não possui registro separado de revogação**: um novo POST consentGranted=true é nova declaração. Exclusão sozinha não impede recriação por cliente que continue enviando. REN-75 deve preservar essa condição; não prometer revogação global inexistente.

Aplicação não registra token/body de metadados. Erros de parsing/validação não repetem payload; SQL é parametrizado. Limites de contrato não substituem operação de implantação. Coleta permanece desabilitada por padrão.

## Erros e retry

| HTTP | Ação do cliente |
| --- | --- |
| 400 | JSON/enum/UUID/intervalo inválido; corrigir contrato |
| 401 | Conta ausente/expirada/desativada; pausar envio, preservar assistência |
| 403 | Sem permissão; não repetir automaticamente |
| 404 | Sessão ausente/alheia/expirada; reconciliar sem trocar proprietário |
| 409 | Coleta desabilitada, quota, chave conflitante ou sessão finalizada; tratar causa |
| 413 | Body maior que 64 KiB; dividir respeitando IDs/lote |
| 415 | Content-Type diferente de application/json |
| 422 | Campo/limite/consentimento inválido |
| 500/rede | Retry limitado com backoff, IDs estáveis e fila vinculada à conta |

Lote retorna somente após confirmar transação. Não remover fila sem recibo válido. Confirmar todos os lotes antes de finalizar; concluir sincronização local apenas após recibo do fechamento. Inferência/áudio nunca aguardam essas operações. Política de retry/UI/produtor real pertence à REN-75.

## Verificação e limites do aceite

```powershell
./scripts/check-toolchain.ps1
./mvnw.cmd "-Dtest=Scan*PostgresIntegrationTest" test
./mvnw.cmd clean verify "-Dspring.profiles.active=test"
```

Java 21, Maven Wrapper 3.9.14, Docker ativo. Suítes cobrem PostgreSQL/Flyway/índices, HTTP real/login/JWT, ownership/RBAC, consentimento, mídia/campos desconhecidos, body chunked, quotas, timestamps, métricas, rollback, concorrência, finalização e exclusão/purga. Dados sintéticos não representam coleta, medição física ou TTS em aparelho.

O DoD offline sem sessão remota depende da REN-75/73. A API não cria dependência no mobile atual. REN-71 fica em prioridade posterior por decisão do responsável em 08/10/2026.
