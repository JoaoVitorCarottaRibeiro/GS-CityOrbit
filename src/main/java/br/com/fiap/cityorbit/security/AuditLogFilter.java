package br.com.fiap.cityorbit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest  request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (isStaticResource(uri)) {
            chain.doFilter(request, response);
            return;
        }

        long   start  = System.currentTimeMillis();
        String ip     = getClientIp(request);
        String user   = getAuthenticatedUser();

        log.info("[AUDIT] {} | ip={} | user={} | {} {}",
                Instant.now(), ip, user, request.getMethod(), uri);

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int  status  = response.getStatus();

            log.info("[AUDIT] {} | ip={} | user={} | {} {} | status={} | {}ms",
                    Instant.now(), ip, user, request.getMethod(), uri, status, elapsed);

            if (status >= 400) {
                log.warn("[AUDIT-ALERT] {} {} → {} | ip={} | user={}",
                        request.getMethod(), uri, status, ip, user);
            }
        }
    }

    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private boolean isStaticResource(String uri) {
        return uri.startsWith("/css/") || uri.startsWith("/js/") ||
               uri.startsWith("/images/") || uri.endsWith(".ico") ||
               uri.endsWith(".html");
    }
}
