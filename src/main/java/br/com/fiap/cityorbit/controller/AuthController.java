package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.dto.AuthRequest;
import br.com.fiap.cityorbit.dto.AuthResponse;
import br.com.fiap.cityorbit.security.JwtUtil;
import br.com.fiap.cityorbit.security.MfaService;
import br.com.fiap.cityorbit.security.TokenBlacklist;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login com suporte a MFA e gerenciamento de sessão JWT")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final TokenBlacklist        tokenBlacklist;
    private final MfaService            mfaService;

    @Operation(
        summary = "Autenticar usuário (Passo 1 de 2 se MFA ativo)",
        description = "Valida credenciais username+senha. Se o usuário tiver MFA ativo, retorna " +
                      "{ mfaRequired: true, sessionId } e o JWT só é emitido após o Passo 2 " +
                      "(POST /api/auth/mfa/complete com o código TOTP). " +
                      "Se MFA não estiver configurado, o JWT é retornado imediatamente."
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String username = auth.getName();
            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.replace("ROLE_", ""))
                    .findFirst().orElse("VIEWER");

            if (mfaService.isMfaEnabled(username)) {
                String sessionId = mfaService.createPendingLogin(username, role);
                log.info("[AUTH] Login parcial (MFA requerido): user={}", username);
                return ResponseEntity.ok(Map.of(
                        "mfaRequired",  true,
                        "sessionId",    sessionId,
                        "mensagem",     "Credenciais válidas. Informe o código TOTP em POST /api/auth/mfa/complete",
                        "proximoPasso", "POST /api/auth/mfa/complete com { sessionId, code }"
                ));
            }

            if ("ADMIN".equals(role)) {
                log.warn("[SECURITY] Admin '{}' autenticado SEM MFA — configure em POST /api/auth/mfa/setup", username);
            }
            String token = jwtUtil.generateToken(username, role);
            log.info("[AUTH] Login bem-sucedido: user={} role={}", username, role);

            if ("ADMIN".equals(role) && !mfaService.isMfaEnabled(username)) {
                return ResponseEntity.ok(Map.of(
                    "token",          token,
                    "username",       username,
                    "role",           role,
                    "expiresInMs",    jwtUtil.getExpirationMs(),
                    "mfaEnabled",     false,
                    "mfaWarning",     "ATENCAO: Usuarios ADMIN devem configurar MFA (ISO 27001 A.8.5). " +
                                      "Configure em POST /api/auth/mfa/setup com este token."
                ));
            }
            return ResponseEntity.ok(new AuthResponse(token, username, role, jwtUtil.getExpirationMs()));

        } catch (BadCredentialsException e) {
            log.warn("[AUTH] Credenciais inválidas: user={}", request.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Credenciais inválidas", "message", "Usuário ou senha incorretos."));
        }
    }

    @Operation(
        summary = "Completar login com MFA (Passo 2 de 2)",
        description = "Valida o código TOTP de 6 dígitos e emite o JWT final. " +
                      "Usar o sessionId retornado pelo POST /api/auth/login. " +
                      "A sessão expira em 5 minutos."
    )
    @PostMapping("/mfa/complete")
    public ResponseEntity<?> mfaComplete(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String codeStr   = body.get("code");

        if (sessionId == null || codeStr == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Campos 'sessionId' e 'code' são obrigatórios"
            ));
        }

        int code;
        try {
            code = Integer.parseInt(codeStr.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Código TOTP deve ser numérico (6 dígitos)"));
        }

        MfaService.PendingSession session = mfaService.completePendingLogin(sessionId, code);
        if (session == null) {
            log.warn("[AUTH] MFA: código inválido ou sessão expirada | sessionId={}", sessionId);
            return ResponseEntity.status(401).body(Map.of(
                    "erro",  "Código TOTP inválido ou sessão expirada (máx. 5 minutos)",
                    "dica",  "Reinicie o processo via POST /api/auth/login"
            ));
        }

        String token = jwtUtil.generateToken(session.getUsername(), session.getRole());
        log.info("[AUTH] Login MFA concluido com sucesso: user={}", session.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, session.getUsername(), session.getRole(), jwtUtil.getExpirationMs()));
    }

    @Operation(summary = "Retorna dados do usuário autenticado (requer token)")
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Não autenticado"));
        }
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .findFirst().orElse("VIEWER");

        return ResponseEntity.ok(Map.of(
                "username",    auth.getName(),
                "role",        role,
                "mfaEnabled",  mfaService.isMfaEnabled(auth.getName()),
                "authenticated", true
        ));
    }

    @Operation(summary = "Logout — revoga o token no servidor")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                long expMs = jwtUtil.extractExpiration(jwt).getTime();
                tokenBlacklist.revoke(jwt, expMs);
                log.info("[AUTH] Token revogado no logout.");
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Logout realizado. Token revogado no servidor."));
    }
}
