package cofre.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Salt de aparência real para usuário inexistente: sempre o mesmo para o mesmo nome,
 * então quem consulta /salt/{user} não consegue distinguir conta existente de inexistente.
 * shortcut: deriva a partir do jwt.secret; usar uma chave própria (salt.secret) se o segredo do JWT for rotacionado.
 */
@Component
public class SaltFalso {

    private static final String ALGORITMO = "HmacSHA256";
    private static final int TAMANHO = 16;

    private final byte[] chave;

    public SaltFalso(@Value("${jwt.secret}") String segredo) {
        this.chave = segredo.getBytes(StandardCharsets.UTF_8);
    }

    public String gerar(String username) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(chave, ALGORITMO));
            byte[] hmac = mac.doFinal(("salt-falso:" + username).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(Arrays.copyOf(hmac, TAMANHO));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Erro ao gerar salt falso", e);
        }
    }
}
