package br.com.fiap.cityorbit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${cityorbit.security.rate-limit.read-rpm:120}")
    private int readRpm;

    @Value("${cityorbit.security.rate-limit.write-rpm:30}")
    private int writeRpm;

    private final ConcurrentHashMap<String, long[]> readCounters  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> writeCounters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        final String ip       = getClientIp(request);
        final boolean isWrite = isWriteMethod(request.getMethod());
        final int limit       = isWrite ? writeRpm : readRpm;
        final ConcurrentHashMap<String, long[]> counters = isWrite ? writeCounters : readCounters;

        if (isRateLimited(counters, ip, limit)) {
            log.warn("[RATE LIMIT] IP {} excedeu {} req/min ({} op) em {}", ip, limit,
                    isWrite ? "escrita" : "leitura", request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Limite de requisições excedido. Tente novamente em breve.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimited(ConcurrentHashMap<String, long[]> counters, String ip, int limit) {
        long now = System.currentTimeMillis();
        counters.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > 60_000L) return new long[]{1L, now};
            v[0]++;
            return v;
        });
        long[] entry = counters.get(ip);
        return entry != null && entry[0] > limit;
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
