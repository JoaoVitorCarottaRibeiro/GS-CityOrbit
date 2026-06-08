package br.com.fiap.cityorbit.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class TokenBlacklist {

    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    private final AtomicBoolean lockdownMode = new AtomicBoolean(false);

    private volatile long lockdownTimestamp = 0L;

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public TokenBlacklist() {
        cleaner.scheduleAtFixedRate(this::purgeExpired, 10, 10, TimeUnit.MINUTES);
    }

    public void revoke(String token, long expirationMs) {
        blacklist.put(token, expirationMs);
        log.warn("[BLACKLIST] Token revogado individualmente. Total na lista: {}", blacklist.size());
    }

    public void revokeAll(String reason) {
        lockdownMode.set(true);
        lockdownTimestamp = System.currentTimeMillis();
        log.error("[INCIDENT-CONTAINMENT] LOCKDOWN ATIVADO — Todos os tokens foram invalidados. Motivo: {}", reason);
    }

    public void releaseLockdown(String reason) {
        lockdownMode.set(false);
        blacklist.clear();
        log.info("[INCIDENT-RECOVERY] Lockdown encerrado. Operação normal restaurada. Motivo: {}", reason);
    }

    public boolean isRevoked(String token) {
        if (lockdownMode.get()) return true;
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (System.currentTimeMillis() > exp) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    public boolean isLockdownActive()   { return lockdownMode.get(); }
    public long    getLockdownTimestamp(){ return lockdownTimestamp; }
    public int     getBlacklistSize()   { return blacklist.size(); }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> now > e.getValue());
        int removed = before - blacklist.size();
        if (removed > 0) log.debug("[BLACKLIST] {} tokens expirados removidos automaticamente.", removed);
    }
}
