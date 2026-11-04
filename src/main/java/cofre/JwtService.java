package cofre;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final String secret;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${jwt.secret:cofre-digital-chave-secreta-zero-trust-jwt-token-2026-pi4}") String secret,
            @Value("${jwt.expiration:86400}") long expirationSeconds,
            ObjectMapper objectMapper) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
    }

    public String gerarToken(Long usuarioId, String username) {
        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String headerBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            long now = Instant.now().getEpochSecond();
            long exp = now + expirationSeconds;

            Map<String, Object> payloadMap = Map.of(
                    "sub", String.valueOf(usuarioId),
                    "usuarioId", usuarioId,
                    "username", username != null ? username : "",
                    "iat", now,
                    "exp", exp
            );
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = headerBase64 + "." + payloadBase64;
            String signature = assinar(content, secret);

            return content + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    public Optional<Long> extrairUsuarioId(String tokenHeaderOuPuro) {
        if (tokenHeaderOuPuro == null || tokenHeaderOuPuro.isBlank()) {
            return Optional.empty();
        }

        String token = tokenHeaderOuPuro.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String content = parts[0] + "." + parts[1];
            String expectedSignature = assinar(content, secret);

            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode jsonNode = objectMapper.readTree(payloadBytes);

            if (jsonNode.has("exp")) {
                long exp = jsonNode.get("exp").asLong();
                if (Instant.now().getEpochSecond() > exp) {
                    return Optional.empty();
                }
            }

            if (jsonNode.has("usuarioId")) {
                return Optional.of(jsonNode.get("usuarioId").asLong());
            } else if (jsonNode.has("sub")) {
                return Optional.of(Long.parseLong(jsonNode.get("sub").asText()));
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean isTokenValido(String tokenHeaderOuPuro) {
        return extrairUsuarioId(tokenHeaderOuPuro).isPresent();
    }

    private String assinar(String content, String secretKey) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}

