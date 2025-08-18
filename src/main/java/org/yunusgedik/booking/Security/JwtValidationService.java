package org.yunusgedik.booking.Security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

@Service
public class JwtValidationService {

    private final JwtPublicKeyProvider keyProvider;

    public JwtValidationService(JwtPublicKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(keyProvider.getPublicKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}