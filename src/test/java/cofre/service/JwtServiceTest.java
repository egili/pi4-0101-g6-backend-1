package cofre.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JwtServiceTest {

    private final JwtService jwt = new JwtService("chave-de-teste-do-cofre-com-mais-de-32-caracteres", 86400L, new ObjectMapper());

    @Test
    public void tokenDoLoginVoltaOUsuarioIdParaAsRotasAutenticadas() {
        String token = jwt.gerarToken(7L, "eliseu");

        assertEquals(7L, jwt.extrairUsuarioId("Bearer " + token).orElseThrow());
    }

    @Test
    public void tokenAssinadoComOutraChaveEhRecusado() {
        JwtService outra = new JwtService("outra-chave-de-teste-com-mais-de-32-caracteres", 86400L, new ObjectMapper());

        assertTrue(outra.extrairUsuarioId(jwt.gerarToken(7L, "eliseu")).isEmpty());
    }
}