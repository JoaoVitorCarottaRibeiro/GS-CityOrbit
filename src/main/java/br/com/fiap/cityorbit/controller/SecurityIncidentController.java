package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.model.IncidentEventEntity;
import br.com.fiap.cityorbit.repository.IncidentEventRepository;
import br.com.fiap.cityorbit.security.TokenBlacklist;
import br.com.fiap.cityorbit.service.IncidentNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/security/incident")
@RequiredArgsConstructor
@Tag(name = "Plano de Resposta a Incidentes",
     description = "Contenção, erradicação e recuperação de incidentes — ISO 27001 A.5.26 | NIST SP 800-61 | LGPD Art.48")
public class SecurityIncidentController {

    private final TokenBlacklist                tokenBlacklist;
    private final IncidentEventRepository       eventRepository;
    private final IncidentNotificationService   notificationService;

    private volatile IncidentStatus currentStatus        = IncidentStatus.NORMAL;
    private volatile String         activeIncidentReason = null;
    private volatile Instant        incidentStartTime    = null;

    @PostConstruct
    void restoreStateFromDatabase() {
        eventRepository.findFirstByOrderByTimestampDesc().ifPresent(latest -> {
            String phase = latest.getPhase();
            if ("CONTAINMENT".equals(phase) || "ERADICATION".equals(phase)) {
                currentStatus = IncidentStatus.valueOf(phase);
                incidentStartTime = latest.getTimestamp();
                activeIncidentReason = latest.getDescription();
                tokenBlacklist.revokeAll("Restauração de estado após reinicialização — incidente ativo");
                log.error("[INCIDENT-RESTORE] Estado de incidente restaurado: {} | início: {}",
                        phase, incidentStartTime);
            }
        });
    }

    @Operation(
        summary = "FASE 1 — Contenção: Ativar lockdown total",
        description = "Revoga TODOS os tokens JWT ativos, desconectando todos os usuários imediatamente. " +
                      "O estado é persistido no banco — um restart do servidor mantém o lockdown ativo. " +
                      "Somente ADMIN pode acionar. Tempo esperado de execução: < 1 minuto."
    )
    @PostMapping("/contain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> contain(@RequestBody Map<String, String> body) {
        String reason     = body.getOrDefault("reason",     "Incidente de segurança detectado");
        String reportedBy = body.getOrDefault("reportedBy", "admin");

        tokenBlacklist.revokeAll(reason);
        currentStatus        = IncidentStatus.CONTAINMENT;
        incidentStartTime    = Instant.now();
        activeIncidentReason = reason;

        IncidentEventEntity event = eventRepository.save(new IncidentEventEntity(
                "CONTAINMENT", reason, reportedBy, Instant.now(),
                "LOCKDOWN ativado. Todos os tokens JWT foram revogados. " +
                "Sistema em modo de contenção. Estado persistido no banco de dados."
        ));

        notificationService.notifyContainment(reason, reportedBy, incidentStartTime);

        log.error("══════════════════════════════════════════════");
        log.error("  INCIDENTE DE SEGURANCA — FASE 1: CONTENCAO ");
        log.error("  Motivo: {}", reason);
        log.error("  Reportado por: {}", reportedBy);
        log.error("  Timestamp: {} | EventId: {}", Instant.now(), event.getId());
        log.error("  Acao: TODOS OS TOKENS JWT FORAM REVOGADOS");
        log.error("══════════════════════════════════════════════");

        return ResponseEntity.ok(Map.of(
            "fase",       "CONTENÇÃO",
            "status",     "LOCKDOWN_ATIVO",
            "eventId",    event.getId(),
            "mensagem",   "Todos os tokens JWT foram revogados. Estado persistido — resistente a reinicialização.",
            "acoes", List.of(
                "✓ Tokens JWT revogados (blacklist global ativada)",
                "✓ Evento persistido no banco de dados (ID: " + event.getId() + ")",
                "✓ Modo de monitoramento elevado ativo",
                "→ Próximo passo: POST /api/security/incident/eradicate após identificar causa raiz"
            ),
            "timestamp", Instant.now().toString()
        ));
    }

    @Operation(
        summary = "FASE 2 — Erradicação: Registrar causa raiz e ações corretivas",
        description = "Documenta a causa raiz e as ações de erradicação. O lockdown permanece ativo. " +
                      "Deve ser executado APÓS análise dos logs de auditoria (GET /audit-events). " +
                      "Tempo esperado: 30 minutos a 4 horas."
    )
    @PostMapping("/eradicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eradicate(@RequestBody Map<String, String> body) {
        if (currentStatus == IncidentStatus.NORMAL) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Nenhum incidente ativo. Acione primeiro a Contenção (POST /contain)."
            ));
        }

        String rootCause    = body.getOrDefault("rootCause",    "Causa raiz não especificada");
        String actionsTaken = body.getOrDefault("actionsTaken", "Ações não especificadas");
        String performedBy  = body.getOrDefault("performedBy",  "admin");

        currentStatus = IncidentStatus.ERADICATION;

        IncidentEventEntity event = eventRepository.save(new IncidentEventEntity(
                "ERADICATION",
                "Causa raiz: " + rootCause + " | Ações: " + actionsTaken,
                performedBy, Instant.now(),
                "Fase de erradicação registrada. Lockdown mantido até recuperação autorizada."
        ));

        notificationService.notifyEradication(rootCause, actionsTaken, performedBy);

        log.error("══════════════════════════════════════════════");
        log.error("  INCIDENTE DE SEGURANCA — FASE 2: ERRADICACAO");
        log.error("  Causa raiz: {}", rootCause);
        log.error("  Acoes: {} | Executado por: {} | EventId: {}", actionsTaken, performedBy, event.getId());
        log.error("══════════════════════════════════════════════");

        return ResponseEntity.ok(Map.of(
            "fase",              "ERRADICAÇÃO",
            "status",            "EM_ERRADICACAO",
            "eventId",           event.getId(),
            "causaRaiz",         rootCause,
            "acoesRegistradas",  actionsTaken,
            "checklist", List.of(
                "✓ Causa raiz documentada e persistida (ID: " + event.getId() + ")",
                "→ Analisar logs: GET /api/security/incident/audit-events",
                "→ Rotacionar segredo JWT (variável CITYORBIT_JWT_SECRET)",
                "→ Rotacionar senhas (CITYORBIT_ADMIN_PASS, CITYORBIT_VIEWER_PASS)",
                "→ Reconfigurar MFA comprometido: DELETE /api/auth/mfa/disable",
                "→ Bloquear IPs maliciosos no firewall/WAF externo",
                "→ Após erradicação: POST /api/security/incident/recover"
            ),
            "lockdownAtivo",     true,
            "timestamp",         Instant.now().toString()
        ));
    }

    @Operation(
        summary = "FASE 3 — Recuperação: Restaurar operação normal",
        description = "Encerra o lockdown e restaura operação. Execute SOMENTE após confirmação de erradicação completa. " +
                      "Gera automaticamente o relatório pós-incidente. Tempo esperado: 2 a 24 horas após erradicação."
    )
    @PostMapping("/recover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recover(@RequestBody Map<String, String> body) {
        if (currentStatus == IncidentStatus.NORMAL) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum incidente ativo para recuperar."));
        }

        String recoveryNotes   = body.getOrDefault("notes",        "Recuperação autorizada");
        String authorizedBy    = body.getOrDefault("authorizedBy", "admin");
        String monitoringLevel = body.getOrDefault("monitoring",   "ELEVADO_72H");

        tokenBlacklist.releaseLockdown("Recuperação autorizada por " + authorizedBy);

        long durationMinutes = incidentStartTime != null
                ? (Instant.now().toEpochMilli() - incidentStartTime.toEpochMilli()) / 60000 : 0;

        IncidentEventEntity event = eventRepository.save(new IncidentEventEntity(
                "RECOVERY",
                "Recuperação autorizada. " + recoveryNotes,
                authorizedBy, Instant.now(),
                "Lockdown encerrado. Operação normal restaurada. Monitoramento: " + monitoringLevel
        ));

        Instant inicio = incidentStartTime;
        currentStatus        = IncidentStatus.NORMAL;
        activeIncidentReason = null;
        incidentStartTime    = null;

        boolean personalDataAffected = body.containsKey("personalDataAffected")
                && Boolean.parseBoolean(body.get("personalDataAffected"));
        notificationService.notifyRecovery(recoveryNotes, authorizedBy, durationMinutes, personalDataAffected);

        log.info("══════════════════════════════════════════════");
        log.info("  INCIDENTE ENCERRADO — FASE 3: RECUPERACAO   ");
        log.info("  Autorizado por: {} | Duracao: {}min | EventId: {}", authorizedBy, durationMinutes, event.getId());
        log.info("══════════════════════════════════════════════");

        return ResponseEntity.ok(Map.of(
            "fase",           "RECUPERAÇÃO",
            "status",         "OPERACAO_NORMAL_RESTAURADA",
            "eventId",        event.getId(),
            "duracaoMinutos", durationMinutes,
            "monitoramento",  monitoringLevel,
            "proximosPassos", List.of(
                "✓ Lockdown encerrado — novos logins permitidos",
                "✓ Blacklist de tokens limpa",
                "✓ Relatório pós-incidente gerado (ver campo 'relatorio')",
                "→ Ativar monitoramento reforçado por 72h",
                "→ Comunicar titulares afetados se aplicável (LGPD Art.48 — prazo 72h)",
                "→ Comunicar ANPD se dados pessoais foram expostos",
                "→ Revisar e atualizar políticas de segurança"
            ),
            "relatorio", Map.of(
                "inicioIncidente",    inicio != null ? inicio.toString() : "N/A",
                "fimIncidente",       Instant.now().toString(),
                "duracaoMinutos",     durationMinutes,
                "totalEventos",       eventRepository.count(),
                "rtoCumprido",        durationMinutes < 240,
                "lgpdArt48Prazo",     "Notificar ANPD e titulares em até 72h se dados pessoais afetados"
            ),
            "timestamp", Instant.now().toString()
        ));
    }

    @Operation(summary = "Status atual do sistema de segurança")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
            "statusAtual",      currentStatus.name(),
            "lockdownAtivo",    tokenBlacklist.isLockdownActive(),
            "tokensRevogados",  tokenBlacklist.getBlacklistSize(),
            "motivoIncidente",  activeIncidentReason != null ? activeIncidentReason : "N/A",
            "inicioIncidente",  incidentStartTime    != null ? incidentStartTime.toString() : "N/A",
            "totalEventosDB",   eventRepository.count(),
            "timestamp",        Instant.now().toString()
        ));
    }

    @Operation(summary = "Histórico de eventos de incidentes (últimos 20) — persistido em banco de dados")
    @GetMapping("/audit-events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> auditEvents() {
        List<IncidentEventEntity> events = eventRepository.findTop20ByOrderByTimestampDesc();
        return ResponseEntity.ok(Map.of(
            "eventos", events.stream().map(e -> Map.of(
                "id",          e.getId(),
                "fase",        e.getPhase(),
                "descricao",   e.getDescription(),
                "executadoPor",e.getPerformedBy(),
                "timestamp",   e.getTimestamp().toString(),
                "notas",       e.getNotes()
            )).toList(),
            "total",        eventRepository.count(),
            "persistencia", "Banco de dados H2 — resistente a reinicialização (produção: PostgreSQL)"
        ));
    }

    @Operation(
        summary = "Relatório completo do plano de resposta a incidentes",
        description = "Documentação do plano: ativos críticos, vetores de ameaça, fases, controles ISO 27001 e LGPD."
    )
    @GetMapping("/plan")
    public ResponseEntity<?> incidentResponsePlan() {
        return ResponseEntity.ok(Map.of(
            "titulo", "Plano de Resposta a Incidentes — CityOrbit",
            "norma",  "ISO 27001 A.5.26 | NIST SP 800-61 Rev.2 | LGPD Art. 48",
            "ativos_criticos", List.of(
                "Chave secreta JWT (HMAC-SHA256) — via variável de ambiente CITYORBIT_JWT_SECRET",
                "Banco de dados H2 com dados de cidades e simulações",
                "Segredos TOTP de MFA dos usuários",
                "Integração NASA POWER API (fonte de dados climáticos)",
                "Imagens de satélite ESRI (camada de mapa)",
                "Logs de auditoria (rastreabilidade)"
            ),
            "vetores_de_ataque", List.of(
                Map.of("vetor","Força bruta/Credential Stuffing","mitigacao","Rate Limiting 30 req/min + BCrypt(12)"),
                Map.of("vetor","Interceptação MitM","mitigacao","HTTPS + HSTS max-age=31536000 + JWT assinado"),
                Map.of("vetor","JWT forjado (alg:none)","mitigacao","HMAC-SHA256 obrigatório + blacklist + expiração"),
                Map.of("vetor","DDoS","mitigacao","Rate Limiting por IP (120 leitura / 30 escrita req/min)"),
                Map.of("vetor","Clickjacking/XSS","mitigacao","X-Frame-Options + CSP + X-Content-Type-Options"),
                Map.of("vetor","Escalonamento de privilégio","mitigacao","RBAC: DELETE=ADMIN; @PreAuthorize por método"),
                Map.of("vetor","Bypass do 2º fator","mitigacao","Sessão pendente MFA com TTL de 5 min; TOTP com janela de 30s"),
                Map.of("vetor","Injeção SQL","mitigacao","JPA com queries parametrizadas; Bean Validation nas entradas")
            ),
            "fases_resposta", Map.of(
                "fase1_contencao", Map.of(
                    "objetivo","Isolar o sistema — impedir propagação do ataque",
                    "tempo_esperado","< 1 minuto",
                    "endpoint","POST /api/security/incident/contain",
                    "acoes", List.of(
                        "Revogar TODOS os tokens JWT (lockdown global)",
                        "Estado persistido em banco — resistente a restart",
                        "Logs de auditoria passam a registrar cada tentativa",
                        "Bloquear IPs suspeitos no firewall externo"
                    )
                ),
                "fase2_erradicacao", Map.of(
                    "objetivo","Remover a causa raiz do incidente",
                    "tempo_esperado","30 minutos a 4 horas",
                    "endpoint","POST /api/security/incident/eradicate",
                    "acoes", List.of(
                        "Analisar GET /api/security/incident/audit-events",
                        "Rotacionar CITYORBIT_JWT_SECRET e senhas de usuários",
                        "Desativar e reemitir segredos TOTP comprometidos",
                        "Aplicar patches e novo deploy da aplicação"
                    )
                ),
                "fase3_recuperacao", Map.of(
                    "objetivo","Restaurar operação normal com segurança aumentada",
                    "tempo_esperado","2 a 24 horas",
                    "endpoint","POST /api/security/incident/recover",
                    "acoes", List.of(
                        "Encerrar lockdown após confirmação de erradicação",
                        "Verificar integridade dos dados",
                        "Ativar monitoramento reforçado por 72h",
                        "Notificar ANPD e titulares afetados (LGPD Art.48 — 72h)"
                    )
                )
            ),
            "metricas", Map.of(
                "RTO","< 4 horas",
                "RPO","< 1 hora",
                "MTTD","< 5 minutos (via alertas do AuditLogFilter)",
                "MTTC","< 1 minuto (lockdown automático)"
            ),
            "iso_27001_controles", List.of(
                "A.5.15 — Controle de acesso (RBAC + MFA)",
                "A.5.16 — Gerenciamento de identidade (JWT + BCrypt + TOTP)",
                "A.5.26 — Resposta a incidentes (este plano — 3 fases)",
                "A.5.29 — Continuidade: estado persistido em banco de dados",
                "A.5.34 — Privacidade: endpoints LGPD implementados",
                "A.8.5  — Autenticação segura (MFA TOTP RFC 6238)",
                "A.8.8  — Gestão de vulnerabilidades (HSTS, CSP, rate limiting)",
                "A.8.15 — Log de eventos (AuditLogFilter — 100% das requisições)",
                "A.8.24 — Criptografia (HMAC-SHA256, BCrypt-12, TLS)"
            ),
            "lgpd_compliance", Map.of(
                "art_7",  "Base legal: legítimo interesse (simulações) + obrigação legal (logs)",
                "art_9",  "Aviso de privacidade: GET /api/lgpd/privacy-notice",
                "art_18", "Direitos do titular: GET/DELETE /api/lgpd/my-data + POST /api/lgpd/consent",
                "art_46", "Medidas técnicas: MFA, BCrypt, HSTS, CSP, rate limiting, audit log",
                "art_48", "Comunicação de incidentes: notificação ANPD em até 72h via relatório pós-incidente"
            )
        ));
    }

    @Operation(summary = "Historico de notificacoes de incidentes (persistido no banco)")
    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> notifications() {
        return ResponseEntity.ok(Map.of(
            "resumo",         notificationService.getSummary(),
            "notificacoes",   notificationService.findAll().stream().map(n -> Map.of(
                "id",           n.getId(),
                "fase",         n.getIncidentPhase(),
                "canal",        n.getChannel(),
                "destinatario", n.getRecipient(),
                "enviado",      n.getSentAt().toString(),
                "entregue",     n.isDelivered(),
                "nota",         n.getDeliveryNote()
            )).toList(),
            "pendentes",      notificationService.findPending().size()
        ));
    }

    enum IncidentStatus { NORMAL, CONTAINMENT, ERADICATION, RECOVERY }
}
