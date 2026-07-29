package co.com.galfields.pos.config;

import co.com.galfields.pos.exception.ErrorResponseBody;
import co.com.galfields.pos.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

// The real SecurityFilterChain for spec 01-login-empleados-roles - replaces
// Spring Boot's default HTTP-Basic-everything lockdown that's been in place
// since spring-boot-starter-security was added (step 1/4). The endpoint ->
// authorization mapping below is copied 1:1 from the spec's Data model
// table ("Mapeo endpoint -> autorización requerida") - that table is the
// single reference for this, per the spec's own Risks note (a misconfigured
// chain silently leaves a sensitive endpoint unprotected), so any endpoint
// not explicitly listed there falls through to denyAll(), not an implicit
// "authenticated is enough".
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN"; // can_login_mobile
    private static final String DESKTOP = "DESKTOP"; // can_login_desktop
    private static final String PERM_POS = "PERM_pos";
    private static final String PERM_SYNC = "PERM_sync";
    private static final String PERM_REPORTES = "PERM_reportes";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // Carved out of the broader ADMIN-only bucket below (must come first -
                        // authorizeHttpRequests matches in declaration order): a desktop terminal
                        // (Cajero-only login, never ADMIN) pulls its own assigned range through this
                        // one read endpoint (invoice_numbering.rs's sync_invoice_numbering) - the rest
                        // of this prefix (create/edit/list ranges) is genuinely admin-only management.
                        .requestMatchers(HttpMethod.GET, "/api/invoice-numbering-ranges/by-terminal/**")
                        .hasAnyAuthority(ADMIN, DESKTOP)

                        .requestMatchers("/api/employees/**", "/api/employee-roles/**", "/api/terminals/**",
                                "/api/invoice-numbering-ranges/**").hasAuthority(ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/reports-access-code").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/reports-access-code/validate").hasAuthority(PERM_REPORTES)

                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/payment-methods/**")
                        .hasAnyAuthority(ADMIN, PERM_SYNC)
                        .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**", "/api/brands/**",
                                "/api/locations/**", "/api/payment-methods/**", "/api/product-variants/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/categories/**", "/api/brands/**",
                                "/api/locations/**", "/api/payment-methods/**", "/api/product-variants/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**", "/api/brands/**",
                                "/api/locations/**", "/api/payment-methods/**", "/api/product-variants/**").hasAuthority(ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/inventory/adjustments").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/reports/inventory", "/api/reports/low-stock").hasAuthority(ADMIN)

                        // Same authority as recording a sale for /cancel and /by-client-event/**/cancel too -
                        // not itemized separately in the spec's table, but they're the same feature area and
                        // a cashier who can record a sale should be the one who can void it.
                        .requestMatchers(HttpMethod.POST, "/api/sales/**").hasAuthority(PERM_POS)

                        .requestMatchers(HttpMethod.GET, "/api/reports/sales-summary", "/api/reports/sales-by-payment-method",
                                "/api/reports/invoices", "/api/reports/invoices/**").hasAnyAuthority(ADMIN, PERM_REPORTES)

                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponseBody.build(status, message)));
    }
}
