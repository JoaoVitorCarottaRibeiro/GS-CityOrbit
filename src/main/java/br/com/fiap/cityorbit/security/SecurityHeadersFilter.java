package br.com.fiap.cityorbit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");

        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' cdn.jsdelivr.net unpkg.com cdnjs.cloudflare.com; " +
                "style-src 'self' 'unsafe-inline' cdn.jsdelivr.net unpkg.com cdnjs.cloudflare.com fonts.googleapis.com; " +
                "font-src 'self' fonts.gstatic.com cdnjs.cloudflare.com data:; " +
                "img-src 'self' data: blob: *.tile.openstreetmap.org *.arcgisonline.com *.googleapis.com *.gstatic.com; " +
                "connect-src 'self' unpkg.com power.larc.nasa.gov; " +
                "frame-src 'self'; " +
                "object-src 'none'");

        response.setHeader("X-Content-Type-Options", "nosniff");

        response.setHeader("X-Frame-Options", "SAMEORIGIN");

        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        response.setHeader("X-Powered-By", "");

        chain.doFilter(request, response);
    }
}
