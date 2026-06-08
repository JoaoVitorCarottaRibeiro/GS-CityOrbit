package br.com.fiap.cityorbit.config;

import br.com.fiap.cityorbit.security.AuditLogFilter;
import br.com.fiap.cityorbit.security.JwtAuthFilter;
import br.com.fiap.cityorbit.security.RateLimitFilter;
import br.com.fiap.cityorbit.security.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter        jwtAuthFilter;
    private final RateLimitFilter      rateLimitFilter;
    private final SecurityHeadersFilter headersFilter;
    private final AuditLogFilter        auditFilter;

    @Value("${cityorbit.security.users.admin.username}")
    private String adminUsername;

    @Value("${cityorbit.security.users.admin.password}")
    private String adminPassword;

    @Value("${cityorbit.security.users.viewer.username}")
    private String viewerUsername;

    @Value("${cityorbit.security.users.viewer.password}")
    private String viewerPassword;

    @Value("${cityorbit.security.cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/", "/*.html", "/css/**", "/js/**",
                                 "/images/**", "/favicon.ico").permitAll()

                .requestMatchers("/api/auth/login", "/api/auth/mfa/complete").permitAll()

                .requestMatchers("/api/auth/mfa/**").authenticated()

                .requestMatchers("/api/auth/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/lgpd/privacy-notice").permitAll()
                .requestMatchers("/api/lgpd/**").authenticated()

                .requestMatchers(HttpMethod.GET, "/api/security/incident/plan").permitAll()

                .requestMatchers("/ws/**").permitAll()

                .requestMatchers("/h2-console/**").hasRole("ADMIN")

                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                .requestMatchers(HttpMethod.PUT,  "/api/**").authenticated()
                .anyRequest().authenticated()
            )


            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"Não autenticado\"," +
                        "\"message\":\"Token JWT obrigatório. Use POST /api/auth/login.\"}");
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"Acesso negado\"," +
                        "\"message\":\"Permissão insuficiente. Role ADMIN necessária.\"}");
                })
            )

            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))

            .addFilterBefore(rateLimitFilter,   UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(headersFilter,     UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter,     UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(auditFilter,        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        UserDetails viewer = User.builder()
                .username(viewerUsername)
                .password(passwordEncoder().encode(viewerPassword))
                .roles("VIEWER")
                .build();

        return new InMemoryUserDetailsManager(admin, viewer);
    }

    @Bean
    public DaoAuthenticationProvider authProvider(UserDetailsService uds) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
