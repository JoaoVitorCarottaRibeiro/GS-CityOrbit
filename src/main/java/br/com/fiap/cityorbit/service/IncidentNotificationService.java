package br.com.fiap.cityorbit.service;

import br.com.fiap.cityorbit.model.IncidentNotificationEntity;
import br.com.fiap.cityorbit.repository.IncidentNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentNotificationService {

    private final IncidentNotificationRepository notificationRepository;

    @Value("${cityorbit.security.incident.notification.recipients:admin@cityorbit.fiap.br,security@cityorbit.fiap.br}")
    private String recipients;

    @Value("${cityorbit.security.incident.notification.anpd-email:incidentes@anpd.gov.br}")
    private String anpdEmail;

    public void notifyContainment(String reason, String reportedBy, Instant incidentStart) {
        String message = buildContainmentMessage(reason, reportedBy, incidentStart);

        for (String recipient : getRecipientList()) {
            send("CONTAINMENT", "EMAIL", recipient, message);
        }

        log.error("╔══════════════════════════════════════════════════╗");
        log.error("║  NOTIFICACAO — FASE 1: CONTENCAO ATIVADA         ║");
        log.error("║  Motivo: {}                                       ", reason);
        log.error("║  Reportado por: {}                                ", reportedBy);
        log.error("║  Timestamp: {}                                    ", incidentStart);
        log.error("║  Destinatarios notificados: {}                    ", recipients);
        log.error("║  Todos os tokens JWT foram REVOGADOS              ║");
        log.error("╚══════════════════════════════════════════════════╝");
    }

    public void notifyEradication(String rootCause, String actionsTaken, String performedBy) {
        String message = buildEradicationMessage(rootCause, actionsTaken, performedBy);

        for (String recipient : getRecipientList()) {
            send("ERADICATION", "EMAIL", recipient, message);
        }

        log.error("╔══════════════════════════════════════════════════╗");
        log.error("║  NOTIFICACAO — FASE 2: ERRADICACAO               ║");
        log.error("║  Causa raiz: {}                                   ", rootCause);
        log.error("║  Acoes: {}                                        ", actionsTaken);
        log.error("║  Executado por: {}                                ", performedBy);
        log.error("╚══════════════════════════════════════════════════╝");
    }

    public void notifyRecovery(String notes, String authorizedBy,
                               long durationMinutes, boolean personalDataAffected) {
        String message = buildRecoveryMessage(notes, authorizedBy, durationMinutes);

        for (String recipient : getRecipientList()) {
            send("RECOVERY", "EMAIL", recipient, message);
        }

        if (personalDataAffected) {
            String anpdMessage = buildAnpdNotification(notes, durationMinutes);
            send("RECOVERY", "ANPD_REPORT", anpdEmail, anpdMessage);

            log.error("╔══════════════════════════════════════════════════╗");
            log.error("║  NOTIFICACAO ANPD — LGPD Art. 48                 ║");
            log.error("║  Dados pessoais podem ter sido afetados.          ║");
            log.error("║  Prazo: notificacao a ANPD em ate 72 horas.       ║");
            log.error("║  Email ANPD: {}                                   ", anpdEmail);
            log.error("║  ACAO NECESSARIA: enviar relatorio formal a ANPD  ║");
            log.error("╚══════════════════════════════════════════════════╝");
        }

        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  NOTIFICACAO — FASE 3: RECUPERACAO               ║");
        log.info("║  Duracao do incidente: {} minutos                 ", durationMinutes);
        log.info("║  Autorizado por: {}                               ", authorizedBy);
        log.info("║  Sistema restaurado a operacao normal             ║");
        log.info("╚══════════════════════════════════════════════════╝");
    }

    public List<IncidentNotificationEntity> findAll() {
        return notificationRepository.findTop50ByOrderBySentAtDesc();
    }

    public List<IncidentNotificationEntity> findPending() {
        return notificationRepository.findByDeliveredFalseOrderBySentAtDesc();
    }

    public Map<String, Object> getSummary() {
        long total   = notificationRepository.count();
        long pending = notificationRepository.findByDeliveredFalseOrderBySentAtDesc().size();
        return Map.of(
            "totalNotificacoes",    total,
            "pendentes",            pending,
            "entregues",            total - pending,
            "destinatarios",        recipients,
            "anpdEmail",            anpdEmail,
            "nota",                 "Em producao: integrar com SMTP / AWS SNS / webhook para entrega real",
            "referenciaLegal",      "LGPD Art. 48 — prazo de 72h para notificar ANPD e titulares afetados"
        );
    }

    private String buildContainmentMessage(String reason, String reportedBy, Instant start) {
        return "[ALERTA CRITICO - INCIDENTE DE SEGURANCA]\n\n" +
               "FASE 1: CONTENCAO ATIVADA\n" +
               "Sistema: CityOrbit - FIAP Global Solution 2026\n" +
               "Timestamp: " + start + "\n" +
               "Motivo: " + reason + "\n" +
               "Reportado por: " + reportedBy + "\n\n" +
               "ACOES EXECUTADAS AUTOMATICAMENTE:\n" +
               "- Todos os tokens JWT foram revogados\n" +
               "- Sistema em modo de contencao\n" +
               "- Nenhum usuario pode autenticar ate a recuperacao\n\n" +
               "PROXIMOS PASSOS:\n" +
               "1. Analise os logs: GET /api/security/incident/audit-events\n" +
               "2. Identifique a causa raiz\n" +
               "3. Execute: POST /api/security/incident/eradicate\n\n" +
               "ISO 27001 A.5.26 | NIST SP 800-61 | LGPD Art. 48";
    }

    private String buildEradicationMessage(String rootCause, String actions, String by) {
        return "[INCIDENTE DE SEGURANCA - ATUALIZACAO]\n\n" +
               "FASE 2: ERRADICACAO EM ANDAMENTO\n" +
               "Sistema: CityOrbit\n" +
               "Timestamp: " + Instant.now() + "\n" +
               "Causa raiz identificada: " + rootCause + "\n" +
               "Acoes realizadas: " + actions + "\n" +
               "Executado por: " + by + "\n\n" +
               "STATUS: Lockdown mantido. Aguardando confirmacao de erradicacao.\n\n" +
               "PROXIMO PASSO: POST /api/security/incident/recover";
    }

    private String buildRecoveryMessage(String notes, String by, long minutes) {
        return "[INCIDENTE ENCERRADO - SISTEMA RESTAURADO]\n\n" +
               "FASE 3: RECUPERACAO CONCLUIDA\n" +
               "Sistema: CityOrbit\n" +
               "Timestamp: " + Instant.now() + "\n" +
               "Duracao do incidente: " + minutes + " minutos\n" +
               "Autorizado por: " + by + "\n" +
               "Notas: " + notes + "\n\n" +
               "STATUS: Operacao normal restaurada.\n" +
               "ACAO: Ativar monitoramento reforcado por 72h.\n\n" +
               "Gerar Post-Incident Report e arquivar conforme ISO 27001 A.5.26.";
    }

    private String buildAnpdNotification(String notes, long minutes) {
        return "[NOTIFICACAO FORMAL - LGPD Art. 48]\n\n" +
               "Controlador: CityOrbit - FIAP Global Solution 2026\n" +
               "Data/hora do incidente: " + Instant.now() + "\n" +
               "Duracao: " + minutes + " minutos\n\n" +
               "Descricao: Incidente de seguranca com potencial impacto a dados pessoais.\n" +
               "Dados possivelmente afetados: username, logs de auditoria.\n" +
               "Medidas de contencao adotadas: revogacao global de tokens JWT.\n" +
               "Medidas de erradicacao: rotacao de credenciais e patches aplicados.\n\n" +
               "Esta notificacao e enviada em cumprimento ao Art. 48 da LGPD,\n" +
               "dentro do prazo legal de 72 horas apos a ciencia do incidente.\n\n" +
               "Encarregado (DPO): dpo@cityorbit.fiap.br";
    }

    private void send(String phase, String channel, String recipient, String message) {
        boolean delivered;
        String  deliveryNote;

        if ("LOG_AUDIT".equals(channel)) {
            delivered    = true;
            deliveryNote = "Registrado no banco de dados e no audit log";
        } else if ("ANPD_REPORT".equals(channel)) {
            // Em producao: integrar com sistema de envio de relatorio a ANPD
            delivered    = false;
            deliveryNote = "PENDENTE — Em producao: enviar por email oficial a " + recipient;
            log.error("[ANPD-NOTIFY] Notificacao formal pendente para: {} | mensagem: {}", recipient, message);
        } else {
            // EMAIL / WEBHOOK — simulado; em producao usar JavaMailSender ou HttpClient
            delivered    = false;
            deliveryNote = "SIMULADO — Em producao: integrar JavaMailSender (SMTP) para entrega real";
            log.warn("[NOTIFY-{}] Destinatario: {} | {}", channel, recipient, message.substring(0, Math.min(100, message.length())));
        }

        notificationRepository.save(new IncidentNotificationEntity(
                phase, channel, recipient, message, delivered, deliveryNote));
    }

    private List<String> getRecipientList() {
        return Arrays.asList(recipients.split(","));
    }
}
