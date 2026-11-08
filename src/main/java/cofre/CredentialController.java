package cofre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CredentialControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/credentials";
    }

    @Test
    @DisplayName("Deve retornar unauthorized ao tentar listar credenciais sem token")
    void deveRetornarUnauthorizedSemToken() {
        ResponseEntity response = restTemplate.getForEntity(baseUrl, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SESSAO_INVALIDA", response.getBody().get("erro"));
    }

    @Test
    @DisplayName("Deve listar credenciais com sucesso quando o token for válido")
    void deveListarCredenciaisComSucesso() {
        // Gera um token válido para teste (ex: usuarioId 1)
        String token = jwtService.gerarToken(1L, "usuarioTeste");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity response = restTemplate.exchange(baseUrl, HttpMethod.GET, request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("credenciais"));
    }
}