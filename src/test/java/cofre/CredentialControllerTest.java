package cofre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CredentialControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long usuarioIdValido;
    private String tokenValido;

    @BeforeEach
    public void setup() {
        String username = "usuario.cred." + System.nanoTime();
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, username);
            ps.setString(2, "hash123");
            ps.setString(3, "salt123");
            return ps;
        }, chave);

        usuarioIdValido = chave.getKey().longValue();
        tokenValido = jwtService.gerarToken(usuarioIdValido, username);
    }

    @Test
    public void salvaCredencialComSucessoQuandoAutenticado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenValido);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CredentialRequest req = new CredentialRequest(
                "Gmail",
                "eliseu@gmail.com",
                "U2FsdGVkX1+ExemploCifrado==",
                "3a4b5c6d7e8f1234"
        );

        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<Map> resposta = restTemplate.postForEntity("/credentials", entity, Map.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertNotNull(resposta.getBody().get("credencialId"), "Deve retornar o ID da credencial");
        assertEquals("Credencial salva com sucesso", resposta.getBody().get("mensagem"));

        // Verifica persistência e integridade da chave estrangeira no banco
        Long credencialId = ((Number) resposta.getBody().get("credencialId")).longValue();
        Map<String, Object> linha = jdbcTemplate.queryForMap(
                "SELECT * FROM credencial WHERE id = ?",
                credencialId
        );

        assertEquals(usuarioIdValido.intValue(), ((Number) linha.get("usuario_id")).intValue());
        assertEquals("Gmail", linha.get("servico"));
        assertEquals("eliseu@gmail.com", linha.get("usuario_servico"));
        assertEquals("U2FsdGVkX1+ExemploCifrado==", linha.get("dados_cifrados"));
        assertEquals("3a4b5c6d7e8f1234", linha.get("iv"));
    }

    @Test
    public void salvaCredencialNaRotaComPrefixoApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CredentialRequest req = new CredentialRequest(
                "Github",
                "egili",
                "CipherBlob==",
                "IV998877"
        );

        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<Map> resposta = restTemplate.postForEntity("/api/credentials", entity, Map.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertNotNull(resposta.getBody().get("credencialId"));
    }

    @Test
    public void recusaRequisicaoSemToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CredentialRequest req = new CredentialRequest("Gmail", "user@gmail.com", "dados", "iv");
        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/credentials", entity, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("SESSAO_INVALIDA", resposta.getBody().get("erro"));
    }

    @Test
    public void recusaRequisicaoComTokenInvalido() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer token.totalmente.invalido");
        headers.setContentType(MediaType.APPLICATION_JSON);

        CredentialRequest req = new CredentialRequest("Gmail", "user@gmail.com", "dados", "iv");
        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/credentials", entity, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("SESSAO_INVALIDA", resposta.getBody().get("erro"));
    }

    @Test
    public void recusaRequisicaoComCamposVazios() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenValido);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // dadosCifrados vazio
        CredentialRequest req = new CredentialRequest("Gmail", "user@gmail.com", "", "iv");
        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/credentials", entity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("DADOS_INVALIDOS", resposta.getBody().get("erro"));
    }

    @Test
    public void recusaQuandoUsuarioNaoExisteNoBanco() {
        String tokenFantasma = jwtService.gerarToken(999999L, "fantasma");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFantasma);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CredentialRequest req = new CredentialRequest("Gmail", "user@gmail.com", "cifrado", "iv");
        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/credentials", entity, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
        assertEquals("USUARIO_NAO_ENCONTRADO", resposta.getBody().get("erro"));
    }

    @Test
    public void exclusaoCascataDeUsuarioRemoveCredenciais() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenValido);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CredentialRequest req = new CredentialRequest("Outlook", "user@corp.com", "cifrado123", "iv123");
        HttpEntity<CredentialRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resposta = restTemplate.postForEntity("/credentials", entity, Map.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());

        Long credencialId = ((Number) resposta.getBody().get("credencialId")).longValue();

        // Remove o usuário
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", usuarioIdValido);

        // Garante que a credencial foi removida em cascata pelo banco
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credencial WHERE id = ?",
                Integer.class,
                credencialId
        );
        assertEquals(0, count, "A credencial deve ser excluída em cascata quando o usuário for excluído");
    }
}

