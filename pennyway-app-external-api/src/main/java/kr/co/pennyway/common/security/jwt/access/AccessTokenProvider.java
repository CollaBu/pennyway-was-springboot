package kr.co.pennyway.common.security.jwt.access;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import kr.co.infra.common.exception.JwtErrorCode;
import kr.co.infra.common.exception.JwtErrorException;
import kr.co.infra.common.jwt.JwtClaims;
import kr.co.infra.common.jwt.JwtProvider;
import kr.co.infra.common.util.JwtErrorCodeUtil;
import kr.co.pennyway.common.annotation.AccessTokenStrategy;
import kr.co.pennyway.common.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.Provider;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Primary
@Component
@AccessTokenStrategy
public class AccessTokenProvider implements JwtProvider {
    private final SecretKey secretKey;
    private final Duration tokenExpiration;

    public AccessTokenProvider(
        @Value("${jwt.secret-key.access-token}") String jwtSecretKey,
        @Value("${jwt.expiration-time.access-token}") Duration tokenExpiration
    ) {
        final byte[] secretKeyBytes = Base64.getDecoder().decode(jwtSecretKey);
        this.secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
        this.tokenExpiration = tokenExpiration;
    }

    @Override
    public String generateToken(JwtClaims claims) {
        Date now = new Date();

        return Jwts.builder()
                .header().add(createHeader()).and()
                .claims(claims.getClaims())
                .signWith(secretKey)
                .expiration(createExpireDate(now, tokenExpiration.toMillis()))
                .compact();
    }

    @Override
    public JwtClaims getSubInfoFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return AccessTokenClaim.of(claims.get("userId", String.class), claims.get("role", String.class));
    }

    @Override
    public LocalDateTime getExpiryDate(String token) {
        Claims claims = getClaimsFromToken(token);
        return DateUtil.toLocalDateTime(claims.getExpiration());
    }

    @Override
    public boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().before(new Date());
    }

    @Override
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            final JwtErrorCode errorCode = JwtErrorCodeUtil.determineErrorCode(e, JwtErrorCode.FAILED_AUTHENTICATION);

            log.warn("Error code : {}, Error - {},  {}", errorCode, e.getClass(), e.getMessage());
            throw new JwtErrorException(errorCode);
        }
    }

    private Map<String, Object> createHeader() {
        return Map.of("typ", "JWT",
                "alg", "HS256",
                "regDate", System.currentTimeMillis());
    }

    private Date createExpireDate(final Date now, long expirationTime) {
        return new Date(now.getTime() + expirationTime);
    }
}
