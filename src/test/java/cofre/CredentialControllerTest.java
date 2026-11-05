package cofre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

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
        // Configura a fábrica HTTP para evitar o erro de streaming auth do JDK em testes de 401
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    @DisplayName("Deve salvar credencial com sucesso quando token e dados forem válidos")
    void deveSalvarCredencialComSucesso() {
        String token = jwtService.gerarToken(1L, "usuario.teste");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, String> requestBody = Map.of(
                "servico", "GitHub",
                "usuarioServico", "dev_user",
                "dadosCifrados", "dadoCifradoBase64==",
                "iv", "iv1234567890"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Credencial salva com sucesso", response.getBody().get("mensagem"));
        assertNotNull(response.getBody().get("credencialId"));
    }

    @Test
    @DisplayName("Deve recusar requisição quando o token JWT não for fornecido")
    void recusaRequisicaoSemToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of(
                "servico", "GitHub",
                "usuarioServico", "dev_user",
                "dadosCifrados", "dadoCifradoBase64==",
                "iv", "iv1234567890"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Deve recusar requisição quando o token JWT for inválido ou malformado")
    void recusaRequisicaoComTokenInvalido() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("token.jwt.invalido.ou.expirado");

        Map<String, String> requestBody = Map.of(
                "servico", "GitHub",
                "usuarioServico", "dev_user",
                "dadosCifrados", "dadoCifradoBase64==",
                "iv", "iv1234567890"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar erro quando o usuário dono do token não for encontrado no banco")
    void retornaErroQuandoUsuarioNaoEncontrado() {
        // ID 99999 não existe na base H2 de testes
        String tokenInexistente = jwtService.gerarToken(99999L, "fantasma");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenInexistente);

        Map<String, String> requestBody = Map.of(
                "servico", "GitHub",
                "usuarioServico", "dev_user",
                "dadosCifrados", "dadoCifradoBase64==",
                "iv", "iv1234567890"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USUARIO_NAO_ENCONTRADO", response.getBody().get("erro"));
    }
}