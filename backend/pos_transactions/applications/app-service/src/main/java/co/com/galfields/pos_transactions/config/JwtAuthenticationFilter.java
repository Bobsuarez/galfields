package co.com.galfields.pos_transactions.config;

import co.com.galfields.pos_transactions.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mirrors backend/pos's JwtAuthenticationFilter: reads "Authorization:
 * Bearer &lt;jwt&gt;", and if present and valid, populates the
 * SecurityContext so SecurityConfig's authorizeHttpRequests rules can check
 * it — missing/invalid/expired tokens are NOT rejected here, they just leave
 * the request unauthenticated, so a route that doesn't require auth (e.g.
 * POST /api/auth/login) still works with no token, and a route that does
 * gets a clean 401 from SecurityConfig's authenticationEntryPoint rather
 * than an exception bubbling out of this filter.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = jwtTokenService.parseClaims(header.substring(BEARER_PREFIX.length()));
                SecurityContextHolder.getContext().setAuthentication(buildAuthentication(claims));
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private Authentication buildAuthentication(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (Boolean.TRUE.equals(claims.get("canLoginMobile", Boolean.class))) {
            authorities.add(new SimpleGrantedAuthority("ADMIN"));
        }
        if (Boolean.TRUE.equals(claims.get("canLoginDesktop", Boolean.class))) {
            authorities.add(new SimpleGrantedAuthority("DESKTOP"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> permissions = claims.get("permissions", Map.class);
        if (permissions != null) {
            permissions.forEach((module, allowed) -> {
                if (Boolean.TRUE.equals(allowed)) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + module));
                }
            });
        }

        String username = claims.get("username", String.class);
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }
}
