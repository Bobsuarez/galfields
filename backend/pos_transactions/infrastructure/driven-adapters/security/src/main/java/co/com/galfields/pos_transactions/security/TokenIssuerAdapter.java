package co.com.galfields.pos_transactions.security;

import co.com.galfields.pos_transactions.model.employee.TokenClaims;
import co.com.galfields.pos_transactions.model.employee.gateways.TokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenIssuerAdapter implements TokenIssuer {

    private final JwtTokenService jwtTokenService;

    @Override
    public String issueToken(TokenClaims claims) {
        return jwtTokenService.issueToken(claims);
    }
}
