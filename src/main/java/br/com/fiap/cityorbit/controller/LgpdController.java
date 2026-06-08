package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.model.ConsentRecordEntity;
import br.com.fiap.cityorbit.repository.ConsentRecordRepository;
import br.com.fiap.cityorbit.repository.SimulationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lgpd")
@RequiredArgsConstructor
@Tag(name = "LGPD — Privacidade e Direitos do Titular",
     description = "Endpoints que garantem os direitos previstos na Lei Geral de Protecao de Dados (Lei 13.709/2018)")
public class LgpdController {

    private final SimulationRepository   simulationRepository;
    private final ConsentRecordRepository consentRecordRepository;

    @Operation(
        summary = "Art. 18, I/II — Acessar meus dados",
        description = "Retorna todos os dados pessoais associados ao usuario autenticado. LGPD Art. 18, I e II."
    )
    @GetMapping("/my-data")
    public ResponseEntity<?> getMyData(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        String username = auth.getName();

        long simCount = simulationRepository.count();

        consentRecordRepository.findFirstByUsernameOrderByTimestampDesc(username)
                .ifPresent(c -> log.info("[LGPD] Acesso a dados — user={} | consentimento={}", username, c.isGranted()));

        log.info("[LGPD] Solicitacao de acesso a dados: user={}", username);

        return ResponseEntity.ok(Map.of(
            "titulo",          "Relatorio de Dados Pessoais — CityOrbit",
            "base_legal",      "LGPD Art. 18, I e II — Direito de confirmacao e acesso",
            "titular",         username,
            "timestamp",       Instant.now().toString(),
            "dados_coletados", Map.of(
                "identificacao", Map.of(
                    "username",   username,
                    "tipo",       "Pseudonimo (identificador interno)",
                    "finalidade", "Autenticacao e controle de acesso"
                ),
                "logs_de_auditoria", Map.of(
                    "descricao",  "Registros de acesso: IP, metodo HTTP, endpoint e status",
                    "retencao",   "90 dias (operacional) / 1 ano (seguranca)",
                    "base_legal", "LGPD Art. 7, II — Cumprimento de obrigacao legal"
                ),
                "simulacoes", Map.of(
                    "total_no_sistema", simCount,
                    "nota", "Simulacoes sao dados de cidades, nao dados pessoais individuais"
                )
            ),
            "consentimento_persistido", consentRecordRepository
                    .findFirstByUsernameOrderByTimestampDesc(username)
                    .map(c -> Map.of("status", c.isGranted() ? "Concedido" : "Revogado",
                                     "data",   c.getTimestamp().toString(),
                                     "ip",     c.getIpAddress()))
                    .orElse(Map.of("status", "Nao registrado")),
            "seus_direitos", Map.of(
                "acesso",            "GET /api/lgpd/my-data",
                "exclusao",          "DELETE /api/lgpd/my-data",
                "consentimento",     "GET /api/lgpd/consent | POST /api/lgpd/consent",
                "aviso_privacidade", "GET /api/lgpd/privacy-notice"
            )
        ));
    }

    @Operation(
        summary = "Art. 18, VI — Solicitar eliminacao dos meus dados",
        description = "Registra a solicitacao de eliminacao. Prazo de atendimento: 15 dias uteis (LGPD Art. 18, §3)."
    )
    @DeleteMapping("/my-data")
    public ResponseEntity<?> requestDeletion(Authentication auth,
                                             @RequestBody(required = false) Map<String, String> body,
                                             HttpServletRequest request) {
        if (auth == null) return ResponseEntity.status(401).build();
        String username = auth.getName();
        String motivo   = body != null ? body.getOrDefault("motivo", "Nao informado") : "Nao informado";
        String ip       = getClientIp(request);

        consentRecordRepository.save(new ConsentRecordEntity(
                username, false, "EXCLUSAO_SOLICITADA", ip));

        log.warn("[LGPD] Solicitacao de eliminacao: user={} | motivo={} | ip={}", username, motivo, ip);

        return ResponseEntity.accepted().body(Map.of(
            "titulo",      "Solicitacao de Eliminacao Registrada",
            "base_legal",  "LGPD Art. 18, VI — Direito a eliminacao",
            "titular",     username,
            "protocolo",   "LGPD-DEL-" + Instant.now().toEpochMilli(),
            "motivo",      motivo,
            "timestamp",   Instant.now().toString(),
            "prazo",       "Ate 15 dias uteis (LGPD Art. 18, §3)",
            "persistido",  true,
            "processo",    List.of(
                "Solicitacao registrada e protocolada no banco de dados",
                "Identificacao e mapeamento de todos os dados do titular",
                "Eliminacao segura de dados identificadores",
                "Anonimizacao de logs de auditoria (substituicao por hash SHA-256)",
                "Confirmacao de conclusao ao titular",
                "Manutencao de logs obrigatorios (LGPD Art. 16 — excecoes legais)"
            )
        ));
    }

    @Operation(summary = "Consultar registro de consentimento persistido no banco de dados")
    @GetMapping("/consent")
    public ResponseEntity<?> getConsent(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        String username = auth.getName();

        return consentRecordRepository.findFirstByUsernameOrderByTimestampDesc(username)
                .map(record -> ResponseEntity.ok(Map.of(
                    "titular",       username,
                    "consentimento", record.isGranted() ? "Concedido" : "Revogado",
                    "data",          record.getTimestamp().toString(),
                    "finalidades",   record.getPurposes(),
                    "ip_registro",   record.getIpAddress(),
                    "persistido",    true,
                    "base_legal",    "LGPD Art. 8 — Consentimento livre, informado e inequivoco"
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                    "titular",       username,
                    "consentimento", "Nao registrado",
                    "aviso",         "Use POST /api/lgpd/consent para registrar seu consentimento"
                )));
    }

    @Operation(
        summary = "Registrar ou revogar consentimento (persistido no banco de dados)",
        description = "Corpo: { \"granted\": true/false, \"purposes\": [\"autenticacao\", \"auditoria\"] }"
    )
    @PostMapping("/consent")
    public ResponseEntity<?> recordConsent(Authentication auth,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        if (auth == null) return ResponseEntity.status(401).build();
        String username = auth.getName();

        boolean granted = Boolean.TRUE.equals(body.get("granted"));
        @SuppressWarnings("unchecked")
        List<String> purposeList = body.containsKey("purposes")
                ? (List<String>) body.get("purposes")
                : List.of("autenticacao", "auditoria_de_seguranca");
        String purposes = String.join(",", purposeList);
        String ip       = getClientIp(request);

        ConsentRecordEntity record = consentRecordRepository.save(
                new ConsentRecordEntity(username, granted, purposes, ip));

        log.info("[LGPD] Consentimento {} persistido no banco: user={} | id={} | ip={}",
                granted ? "concedido" : "revogado", username, record.getId(), ip);

        return ResponseEntity.ok(Map.of(
            "mensagem",      "Consentimento " + (granted ? "concedido" : "revogado") + " e persistido no banco de dados",
            "titular",       username,
            "consentimento", granted ? "Concedido" : "Revogado",
            "timestamp",     record.getTimestamp().toString(),
            "finalidades",   purposeList,
            "persistido",    true,
            "registroId",    record.getId(),
            "base_legal",    "LGPD Art. 8, §5 — Revogacao de consentimento a qualquer momento"
        ));
    }

    @Operation(summary = "Aviso de privacidade — LGPD Art. 9")
    @GetMapping("/privacy-notice")
    public ResponseEntity<?> privacyNotice() {
        java.util.Map<String, Object> notice = new java.util.LinkedHashMap<>();
        notice.put("controlador",         "CityOrbit - FIAP Global Solution 2026");
        notice.put("finalidade",          "Plataforma de simulacao de impacto climatico em cidades");
        notice.put("base_legal",          "LGPD Art. 7, I (consentimento) e IX (legitimo interesse)");
        notice.put("dados_tratados",      List.of(
                "Username (pseudonimo) - autenticacao",
                "Hash da senha (BCrypt) - seguranca de acesso",
                "Logs de auditoria - seguranca e conformidade legal",
                "Registros de consentimento LGPD - rastreabilidade"
        ));
        notice.put("dados_nao_coletados", List.of("CPF", "e-mail", "localizacao do usuario", "dados biometricos"));
        notice.put("compartilhamento",    "Nenhum dado e compartilhado com terceiros");
        notice.put("retencao",            "Dados: enquanto a conta estiver ativa. Logs: 90 dias / 1 ano (seguranca)");
        notice.put("direitos_do_titular", List.of(
                "Acesso: GET /api/lgpd/my-data",
                "Eliminacao: DELETE /api/lgpd/my-data",
                "Consentimento: POST /api/lgpd/consent",
                "Portabilidade: mediante solicitacao ao controlador"
        ));
        notice.put("encarregado_dpo",     "dpo@cityorbit.fiap.br");
        notice.put("ultima_atualizacao",  "2026-06-03");
        notice.put("versao",              "1.1");
        return ResponseEntity.ok(notice);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
