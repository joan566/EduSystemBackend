package com.edusistem.core.auth.infrastructure.security;

import com.edusistem.core.auth.domain.outputports.TokenIssuerPort;
import com.edusistem.core.auth.domain.vo.IssuedToken;
import com.edusistem.core.authorization.domain.enums.RoleName;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.user.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenIssuerPort {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] secret = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationSeconds = properties.expirationSeconds();
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("ver", user.getTokenVersion())
                .claim("roles", user.getRoles().stream().map(RoleName::name).sorted().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
        return new IssuedToken(token, "Bearer", expirationSeconds);
    }

    public record ParsedToken(AuthenticatedUser user, int version) {
    }

    /** Devuelve el principal y la versión del token si es válido y no expiró; vacío en cualquier otro caso. */
    public Optional<ParsedToken> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            List<?> roles = claims.get("roles", List.class);
            var roleNames = new HashSet<String>();
            if (roles != null) {
                roles.forEach(r -> roleNames.add(String.valueOf(r)));
            }
            Integer version = claims.get("ver", Integer.class);
            if (version == null) {
                return Optional.empty();
            }
            return Optional.of(new ParsedToken(new AuthenticatedUser(Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class), roleNames), version));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
