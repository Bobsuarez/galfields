package co.com.galfields.pos.service;

import co.com.galfields.pos.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Issues and validates employee-session JWTs (spec 01-login-empleados-roles).
@Component
public class JwtService {

    // A day always has a real midnight in this zone (no DST in Colombia) -
    // the spec requires exp to always land there, whatever time the login
    // actually happened at.
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(Employee employee, Long terminalId, Map<String, Boolean> permissions) {
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(employee.getEmployeeId()))
                .claim("username", employee.getUsername())
                .claim("roleId", employee.getRole().getRoleId())
                .claim("roleName", employee.getRole().getRoleName())
                .claim("permissions", permissions)
                // Not in the spec's example claims block, but the
                // SecurityFilterChain (step 7) needs to gate several
                // endpoints on "can_login_mobile" without a DB hit per
                // request, and without falling back to comparing roleName
                // strings (the spec explicitly rejected that - see
                // Decisions). canLoginDesktop is included for symmetry,
                // even though no endpoint currently gates on it.
                .claim("canLoginMobile", employee.getRole().isCanLoginMobile())
                .claim("canLoginDesktop", employee.getRole().isCanLoginDesktop())
                .expiration(Date.from(nextMidnightBogota()))
                .signWith(key);
        // Only present for desktop logins - see LoginRequest/AuthService.
        if (terminalId != null) {
            builder.claim("terminalId", terminalId);
        }
        return builder.compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Instant nextMidnightBogota() {
        return ZonedDateTime.now(BOGOTA)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(BOGOTA)
                .toInstant();
    }
}
