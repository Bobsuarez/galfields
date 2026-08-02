package co.com.galfields.pos_transactions.model.employee.gateways;

import co.com.galfields.pos_transactions.model.employee.TokenClaims;

public interface TokenIssuer {
    String issueToken(TokenClaims claims);
}
