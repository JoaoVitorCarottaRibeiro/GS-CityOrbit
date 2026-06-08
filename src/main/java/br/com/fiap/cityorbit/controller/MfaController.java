package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.security.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA — Autenticação Multi-Fator",
     description = "Configuração e verificação de segundo fator de autenticação TOTP (RFC 6238 / ISO 27001 A.8.5)")
public class MfaController {

    private final MfaService mfaService;

    @Operation(summary = "Verifica se o MFA está habilitado para o usuário autenticado")
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        boolean enabled = mfaService.isMfaEnabled(auth.getName());
        return ResponseEntity.ok(Map.of(
                "username",    auth.getName(),
                "mfaEnabled",  enabled,
                "mensagem",    enabled
                    ? "MFA ativo. Login requer código TOTP após as credenciais."
                    : "MFA inativo. Use POST /api/auth/mfa/setup para ativar."
        ));
    }


    @Operation(
        summary = "Passo 1 — Iniciar configuração do MFA",
        description = "Gera um segredo TOTP único e retorna o QR code para escanear no Google Authenticator ou Authy. " +
                      "O segredo fica pendente por 10 minutos até ser confirmado via POST /activate."
    )
    @PostMapping("/setup")
    public ResponseEntity<?> setup(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        MfaService.MfaSetupResult result = mfaService.setupMfa(auth.getName());

        return ResponseEntity.ok(Map.of(
                "mensagem",    "Escaneie o QR code no Google Authenticator ou Authy e confirme via POST /api/auth/mfa/activate",
                "secret",      result.getSecret(),
                "otpAuthUri",  result.getOtpAuthUri(),
                "qrImageUrl",  result.getQrImageUrl(),
                "instrucoes", Map.of(
                    "passo1", "Abra o Google Authenticator ou Authy no seu celular",
                    "passo2", "Clique em '+' e escaneie o QR code ou insira o segredo manualmente",
                    "passo3", "Use o código de 6 dígitos gerado em POST /api/auth/mfa/activate"
                )
        ));
    }

    @Operation(
        summary = "Passo 2 — Confirmar ativação do MFA com o primeiro código TOTP",
        description = "Valida o primeiro código gerado pelo app autenticador e ativa o MFA permanentemente. " +
                      "Após ativação, todos os logins exigirão o segundo fator."
    )
    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody Map<String, String> body, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        String codeStr = body.get("code");
        if (codeStr == null || codeStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Campo 'code' obrigatório (6 dígitos TOTP)"));
        }

        int code;
        try {
            code = Integer.parseInt(codeStr.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Código deve conter apenas dígitos numéricos"));
        }

        boolean activated = mfaService.activateMfa(auth.getName(), code);
        if (!activated) {
            return ResponseEntity.status(400).body(Map.of(
                    "erro",    "Código TOTP inválido ou setup expirado",
                    "dica",    "Execute novamente POST /api/auth/mfa/setup e use o código dentro de 30 segundos"
            ));
        }

        log.info("[MFA] MFA ativado para: {}", auth.getName());
        return ResponseEntity.ok(Map.of(
                "mensagem",  "MFA ativado com sucesso!",
                "username",  auth.getName(),
                "mfaAtivo",  true,
                "aviso",     "A partir de agora, todos os logins exigirão o código TOTP após a senha."
        ));
    }

    @Operation(
        summary = "Desabilitar MFA de um usuário (somente ADMIN)",
        description = "Remove o segundo fator de autenticação. Use apenas em casos de perda de acesso ao dispositivo."
    )
    @DeleteMapping("/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disable(@RequestBody Map<String, String> body) {
        String targetUser = body.get("username");
        if (targetUser == null || targetUser.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Campo 'username' obrigatório"));
        }

        mfaService.disableMfa(targetUser);
        log.warn("[MFA] MFA desabilitado por admin para: {}", targetUser);

        return ResponseEntity.ok(Map.of(
                "mensagem",  "MFA desabilitado para: " + targetUser,
                "aviso",     "O usuário deve configurar novo MFA em POST /api/auth/mfa/setup"
        ));
    }
}
