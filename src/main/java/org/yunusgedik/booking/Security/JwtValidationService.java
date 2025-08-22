package org.yunusgedik.booking.Security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtValidationService {

    private final JwtPublicKeyProvider keyProvider;

    public JwtValidationService(JwtPublicKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public Long extractUserId(String token) {
        String subject = Jwts.parserBuilder()
            .setSigningKey(keyProvider.getPublicKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
        return Long.parseLong(subject);
    }

    public Set<String> extractRoles(String token) {
        Object rolesClaim = Jwts.parserBuilder()
            .setSigningKey(keyProvider.getPublicKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("roles");

        List<?> rolesList = (List<?>) rolesClaim;
        return rolesList.stream()
            .filter(obj -> obj instanceof String)
            .map(obj -> (String) obj)
            .collect(Collectors.toSet());
    }
}