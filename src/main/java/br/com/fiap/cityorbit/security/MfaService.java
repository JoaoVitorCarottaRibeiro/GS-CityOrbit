package br.com.fiap.cityorbit.security;

import br.com.fiap.cityorbit.model.MfaSecretEntity;
import br.com.fiap.cityorbit.repository.MfaSecretRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final String ISSUER = "CityOrbit";

    private final GoogleAuthenticator   gAuth = new GoogleAuthenticator();
    private final MfaSecretRepository   mfaSecretRepository;

    private final ConcurrentHashMap<String, PendingEnrollment> pendingEnrollments = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, PendingSession> pendingLogins = new ConcurrentHashMap<>();

    public MfaSetupResult setupMfa(String username) {
        GoogleAuthenticatorKey key    = gAuth.createCredentials();
        String                 secret = key.getKey();

        pendingEnrollments.put(username,
                new PendingEnrollment(secret, Instant.now().plus(10, ChronoUnit.MINUTES)));

        String otpAuthUri = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(ISSUER, username, key);
        String qrImageUrl = "https://chart.googleapis.com/chart?chs=250x250&chld=M%7C0&cht=qr&chl="
                + URLEncoder.encode(otpAuthUri, StandardCharsets.UTF_8);

        log.info("[MFA] Setup iniciado para usuario: {}", username);
        return new MfaSetupResult(secret, otpAuthUri, qrImageUrl);
    }

    public boolean activateMfa(String username, int code) {
        PendingEnrollment enrollment = pendingEnrollments.get(username);
        if (enrollment == null || enrollment.isExpired()) {
            log.warn("[MFA] Setup pendente ausente ou expirado: {}", username);
            return false;
        }
        if (!gAuth.authorize(enrollment.getSecret(), code)) {
            log.warn("[MFA] Codigo invalido na ativacao: {}", username);
            return false;
        }

        MfaSecretEntity entity = mfaSecretRepository.findById(username)
                .orElse(new MfaSecretEntity(username, enrollment.getSecret(), false));
        entity.setSecret(enrollment.getSecret());
        entity.setEnabled(true);
        mfaSecretRepository.save(entity);

        pendingEnrollments.remove(username);
        log.info("[MFA] MFA ativado e persistido no banco para: {}", username);
        return true;
    }

    public boolean verifyCode(String username, int code) {
        return mfaSecretRepository.findByUsernameAndEnabledTrue(username)
                .map(entity -> {
                    boolean valid = gAuth.authorize(entity.getSecret(), code);
                    if (!valid) log.warn("[MFA] Codigo TOTP invalido para: {}", username);
                    return valid;
                })
                .orElse(false);
    }

    public boolean isMfaEnabled(String username) {
        return mfaSecretRepository.existsByUsernameAndEnabledTrue(username);
    }

    public void disableMfa(String username) {
        mfaSecretRepository.findById(username).ifPresent(entity -> {
            entity.setEnabled(false);
            mfaSecretRepository.save(entity);
        });
        log.info("[MFA] MFA desabilitado no banco para: {}", username);
    }

    public String createPendingLogin(String username, String role) {
        String sessionId = UUID.randomUUID().toString();
        pendingLogins.put(sessionId,
                new PendingSession(username, role, Instant.now().plus(5, ChronoUnit.MINUTES)));
        log.info("[MFA] Sessao pendente criada: user={} | session={}", username, sessionId);
        return sessionId;
    }

    public PendingSession completePendingLogin(String sessionId, int code) {
        PendingSession session = pendingLogins.get(sessionId);
        if (session == null || session.isExpired()) {
            log.warn("[MFA] SessionId invalido ou expirado: {}", sessionId);
            return null;
        }
        if (!verifyCode(session.getUsername(), code)) return null;
        pendingLogins.remove(sessionId);
        return session;
    }

    public static class MfaSetupResult {
        private final String secret;
        private final String otpAuthUri;
        private final String qrImageUrl;

        public MfaSetupResult(String secret, String otpAuthUri, String qrImageUrl) {
            this.secret     = secret;
            this.otpAuthUri = otpAuthUri;
            this.qrImageUrl = qrImageUrl;
        }

        public String getSecret()     { return secret; }
        public String getOtpAuthUri() { return otpAuthUri; }
        public String getQrImageUrl() { return qrImageUrl; }
    }

    public static class PendingSession {
        private final String  username;
        private final String  role;
        private final Instant expiresAt;

        public PendingSession(String username, String role, Instant expiresAt) {
            this.username  = username;
            this.role      = role;
            this.expiresAt = expiresAt;
        }

        public String  getUsername() { return username; }
        public String  getRole()     { return role; }
        public boolean isExpired()   { return Instant.now().isAfter(expiresAt); }
    }

    private static class PendingEnrollment {
        private final String  secret;
        private final Instant expiresAt;

        PendingEnrollment(String secret, Instant expiresAt) {
            this.secret    = secret;
            this.expiresAt = expiresAt;
        }

        String  getSecret()  { return secret; }
        boolean isExpired()  { return Instant.now().isAfter(expiresAt); }
    }
}
