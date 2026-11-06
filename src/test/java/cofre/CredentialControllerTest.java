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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/credentials";
        // Configura a fábrica HTTP para evitar o erro de streaming auth do JDK em testes de 401
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        jdbcTemplate.execute("DELETE FROM credencial");
    }

    @Test
    @DisplayName("Deve salvar credencial com sucesso quando token e dados forem válidos")
    void deveSalvarCredencialComSucesso() {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, "usuario.controller." + System.nanoTime());
            ps.setString(2, "hashSenha");
            ps.setString(3, "saltAleatorio");
            return ps;
        }, chave);
        String token = jwtService.gerarToken(chave.getKey().longValue(), "usuario.teste");

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

    @Test
    @DisplayName("GET /credentials devolve só os blobs cifrados do usuário do JWT")
    void listaApenasCredenciaisDoUsuarioLogado() {
        Long dono = inserirUsuario("dono.lista");
        Long outro = inserirUsuario("outro.lista");
        inserirCredencial(dono, "Gmail", "eliseu@gmail.com", "blob-gmail", "iv-gmail");
        inserirCredencial(outro, "Banco", "outro@banco.com", "blob-alheio", "iv-alheio");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.gerarToken(dono, "dono.lista"));

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        Map item = (Map) response.getBody().get(0);
        assertEquals("Gmail", item.get("servico"));
        assertEquals("eliseu@gmail.com", item.get("usuarioServico"));
        assertEquals("blob-gmail", item.get("dadosCifrados"));
        assertEquals("iv-gmail", item.get("iv"));
        assertNotNull(item.get("credencialId"));
        assertFalse(response.getBody().toString().contains("blob-alheio"));
    }

    @Test
    @DisplayName("GET /credentials devolve lista vazia quando o usuário não tem credenciais")
    void listaVaziaQuandoUsuarioNaoTemCredenciais() {
        Long usuario = inserirUsuario("sem.credenciais");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.gerarToken(usuario, "sem.credenciais"));

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /credentials recusa token ausente")
    void listarRecusaSemToken() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("SESSAO_INVALIDA", response.getBody().get("erro"));
    }

    @Test
    @DisplayName("GET /credentials recusa token inválido")
    void listarRecusaTokenInvalido() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token.invalido");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("SESSAO_INVALIDA", response.getBody().get("erro"));
    }

    private Long inserirUsuario(String prefixo) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, prefixo + "." + System.nanoTime());
            ps.setString(2, "hashSenha");
            ps.setString(3, "saltAleatorio");
            return ps;
        }, chave);
        return chave.getKey().longValue();
    }

    private void inserirCredencial(Long usuarioId, String servico, String usuarioServico, String dadosCifrados, String iv) {
        jdbcTemplate.update(
                "INSERT INTO credencial (usuario_id, servico, usuario_servico, dados_cifrados, iv) VALUES (?, ?, ?, ?, ?)",
                usuarioId, servico, usuarioServico, dadosCifrados, iv
        );
    }
}