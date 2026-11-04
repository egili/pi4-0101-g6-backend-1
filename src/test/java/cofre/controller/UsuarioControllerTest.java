package cofre.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//dps apagar o disabled
@Disabled("Desativado temporariamente para não quebrar o CI. Requer config de banco.")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UsuarioControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void cadastraUsuarioNovo() {
        Map<String, String> corpo = Map.of("username", "ana.cadastro", "senhaMestraHash", "hash", "salt", "sal");

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/usuarios", corpo, Map.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertNotNull(resposta.getBody().get("usuarioId"));
    }

    @Test
    public void recusaNomeRepetido() {
        Map<String, String> corpo = Map.of("username", "bia.repetido", "senhaMestraHash", "hash", "salt", "sal");
        restTemplate.postForEntity("/usuarios", corpo, Map.class);

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/usuarios", corpo, Map.class);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("USUARIO_JA_EXISTE", resposta.getBody().get("erro"));
    }

    @Test
    public void recusaCampoVazio() {
        Map<String, String> corpo = Map.of("username", "", "senhaMestraHash", "hash", "salt", "sal");

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/usuarios", corpo, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("CAMPO_OBRIGATORIO_AUSENTE", resposta.getBody().get("erro"));
    }
}