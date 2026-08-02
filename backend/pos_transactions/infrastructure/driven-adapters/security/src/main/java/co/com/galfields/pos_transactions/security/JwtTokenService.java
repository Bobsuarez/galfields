package co.com.galfields.pos_transactions.security;

import co.com.galfields.pos_transactions.model.employee.TokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Mirrors backend/pos's JwtService — issues and validates employee-session
 * JWTs. A concrete class (not a domain port itself) since token *parsing*
 * is needed by app-service's JwtAuthenticationFilter (pure Spring Security
 * plumbing, outside the usecase layer) while token *issuing* is needed by
 * AuthUseCase through the {@link TokenIssuerAdapter} port implementation —
 * both share this one signing key/library setup.
 */
@Component
public class JwtTokenService {

    // A day always has a real midnight in this zone (no DST in Colombia) -
    // exp must always land there, whatever time the login actually happened at.
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final SecretKey key;

    public JwtTokenService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(TokenClaims claims) {
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(claims.employeeId()))
                .claim("username", claims.username())
                .claim("roleId", claims.roleId())
                .claim("roleName", claims.roleName())
                .claim("permissions", claims.permissions())
                .claim("canLoginMobile", claims.canLoginMobile())
                .claim("canLoginDesktop", claims.canLoginDesktop())
                .expiration(Date.from(nextMidnightBogota()))
                .signWith(key);
        if (claims.terminalId() != null) {
            builder.claim("terminalId", claims.terminalId());
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
