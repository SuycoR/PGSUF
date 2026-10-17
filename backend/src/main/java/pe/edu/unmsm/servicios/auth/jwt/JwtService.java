package pe.edu.unmsm.servicios.auth.jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import pe.edu.unmsm.servicios.config.AppProperties;
import pe.edu.unmsm.servicios.usuario.entity.Usuario;

/** Issues and validates HS256 tokens. The secret comes from JWT_SECRET (Base64, at least 32 bytes). */
@Service
public class JwtService {

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_CORREO = "correo";

    private final SecretKey key;
    private final Duration duracion;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secret()));
        this.duracion = Duration.ofMinutes(properties.jwt().expirationMinutes());
        this.clock = clock;
    }

    public String generar(Usuario usuario) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim(CLAIM_CORREO, usuario.getCorreo())
                .claim(CLAIM_ROL, usuario.getRol().getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(key)
                .compact();
    }

    /** Returns the user of a valid token, or empty if the token is malformed, tampered or expired. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_CORREO, String.class),
                    claims.get(CLAIM_ROL, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
