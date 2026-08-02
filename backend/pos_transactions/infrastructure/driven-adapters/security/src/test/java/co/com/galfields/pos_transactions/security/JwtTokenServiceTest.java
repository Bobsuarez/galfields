package co.com.galfields.pos_transactions.security;

import co.com.galfields.pos_transactions.model.employee.TokenClaims;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private final JwtTokenService service = new JwtTokenService(
            new JwtProperties("test-secret-at-least-32-bytes-long-1234"));

    @Test
    void issuesAndParsesTokenWithAllClaims() {
        TokenClaims claims = new TokenClaims(1L, "cajero1", 2L, "Cajero",
                Map.of("pos", true, "inventario", false), false, true, 5L);

        String token = service.issueToken(claims);
        Claims parsed = service.parseClaims(token);

        assertThat(parsed.getSubject()).isEqualTo("1");
        assertThat(parsed.get("username", String.class)).isEqualTo("cajero1");
        assertThat(parsed.get("roleId", Long.class)).isEqualTo(2L);
        assertThat(parsed.get("roleName", String.class)).isEqualTo("Cajero");
        assertThat(parsed.get("canLoginMobile", Boolean.class)).isFalse();
        assertThat(parsed.get("canLoginDesktop", Boolean.class)).isTrue();
        assertThat(parsed.get("terminalId", Long.class)).isEqualTo(5L);
    }

    @Test
    void omitsTerminalIdClaimForMobileLogin() {
        TokenClaims claims = new TokenClaims(1L, "admin", 1L, "Administrador", Map.of(), true, false, null);

        String token = service.issueToken(claims);
        Claims parsed = service.parseClaims(token);

        assertThat(parsed.get("terminalId")).isNull();
    }
}
