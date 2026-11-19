package oryon.springboot.ozone.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import oryon.springboot.ozone.entities.User;
import io.jsonwebtoken.Claims;
import oryon.springboot.ozone.exception.UnauthorizedException;

import java.util.UUID;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret:ozoneSuperSecretKeyForJwtAuthentication2026MustBeLongEnough}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("phone", user.getPhone())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public UUID extractUserId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public String generateMfaToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("purpose", "mfa_setup")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000)) // 10 minutes
                .signWith(key)
                .compact();
    }

    public UUID validateMfaToken(String mfaToken) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(mfaToken)
                    .getPayload();
            if (!"mfa_setup".equals(claims.get("purpose", String.class))) {
                throw new IllegalArgumentException("Jeton MFA invalide");
            }
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Jeton MFA invalide ou expiré");
        }
    }

    public String generateMfaToken(User user, String purpose) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("purpose", purpose)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000)) // 10 minutes
                .signWith(key)
                .compact();
    }

    public UUID validateMfaToken(String mfaToken, String expectedPurpose) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(mfaToken)
                    .getPayload();

            String purpose = claims.get("purpose", String.class);
            if (!expectedPurpose.equals(purpose)) {
                throw new UnauthorizedException("Jeton MFA invalide");
            }
            return UUID.fromString(claims.getSubject());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Jeton MFA invalide ou expiré");
        }
    }

}
