package cofre;

import cofre.service.JwtService;

import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de Integração e Concorrência para a Task H07.3 (Back-end 1 do Cofre).
 *
 * Valida:
 * 1. Simulação de dois usuários autenticados simultaneamente com tokens JWT distintos.
 * 2. Operações concorrentes de salvamento (POST /credentials) e listagem (GET /credentials).
 * 3. Isolamento estrito de dados: nenhum usuário enxerga dados do outro (Zero data leakage).
 * 4. Não-travamento do servidor HTTP e integridade transacional sob concorrência real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationConcorrenciaTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;
    private ExecutorService executorService;

    private Long usuarioIdA;
    private String tokenA;

    private Long usuarioIdB;
    private String tokenB;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/credentials";
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        executorService = Executors.newFixedThreadPool(4);

        // Cria Usuario A
        String usernameA = "usuarioA_" + System.nanoTime();
        usuarioIdA = criarUsuario(usernameA);
        tokenA = jwtService.gerarToken(usuarioIdA, usernameA);

        // Cria Usuario B
        String usernameB = "usuarioB_" + System.nanoTime();
        usuarioIdB = criarUsuario(usernameB);
        tokenB = jwtService.gerarToken(usuarioIdB, usernameB);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("H07.3 - Dois usuários consultam suas credenciais de forma concorrente sem travamento e com isolamento total")
    void deveListarCredenciaisConcorrentementeComIsolamentoTotal() throws Exception {
        // 1. Prepara a base salvando credenciais distintas para cada usuário
        salvarCredencial(tokenA, "GitHub", "usuarioA@github.com", "cipherA_github==", "ivA1");
        salvarCredencial(tokenA, "ProtonMail", "usuarioA@proton.me", "cipherA_proton==", "ivA2");

        salvarCredencial(tokenB, "Google", "usuarioB@gmail.com", "cipherB_google==", "ivB1");
        salvarCredencial(tokenB, "AWS", "usuarioB@aws.amazon.com", "cipherB_aws==", "ivB2");

        // 2. Prepara requisições GET simultâneas através de CountDownLatch
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<ResponseEntity<Map>> tarefaConsultaA = () -> {
            startLatch.await(5, TimeUnit.SECONDS);
            HttpEntity<Void> entity = new HttpEntity<>(criarHeaders(tokenA));
            return restTemplate.exchange(baseUrl, HttpMethod.GET, entity, Map.class);
        };

        Callable<ResponseEntity<Map>> tarefaConsultaB = () -> {
            startLatch.await(5, TimeUnit.SECONDS);
            HttpEntity<Void> entity = new HttpEntity<>(criarHeaders(tokenB));
            return restTemplate.exchange(baseUrl, HttpMethod.GET, entity, Map.class);
        };

        Future<ResponseEntity<Map>> futuroA = executorService.submit(tarefaConsultaA);
        Future<ResponseEntity<Map>> futuroB = executorService.submit(tarefaConsultaB);

        // 3. Libera as threads simultaneamente
        startLatch.countDown();

        ResponseEntity<Map> respostaA = futuroA.get(10, TimeUnit.SECONDS);
        ResponseEntity<Map> respostaB = futuroB.get(10, TimeUnit.SECONDS);

        // 4. Asserções do Usuário A
        assertEquals(HttpStatus.OK, respostaA.getStatusCode(), "Status da consulta do Usuário A deve ser 200 OK");
        assertNotNull(respostaA.getBody());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listaA = (List<Map<String, Object>>) respostaA.getBody().get("credenciais");
        assertNotNull(listaA, "Lista de credenciais do Usuário A não deve ser nula");
        assertEquals(2, listaA.size(), "Usuário A deve receber exatamente suas 2 credenciais");

        List<String> servicosA = listaA.stream().map(m -> (String) m.get("servico")).toList();
        assertTrue(servicosA.contains("GitHub"), "Usuário A deve ver a credencial GitHub");
        assertTrue(servicosA.contains("ProtonMail"), "Usuário A deve ver a credencial ProtonMail");
        assertFalse(servicosA.contains("Google"), "Usuário A NUNCA deve ver dados de outros usuários (Google)");
        assertFalse(servicosA.contains("AWS"), "Usuário A NUNCA deve ver dados de outros usuários (AWS)");

        // 5. Asserções do Usuário B
        assertEquals(HttpStatus.OK, respostaB.getStatusCode(), "Status da consulta do Usuário B deve ser 200 OK");
        assertNotNull(respostaB.getBody());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listaB = (List<Map<String, Object>>) respostaB.getBody().get("credenciais");
        assertNotNull(listaB, "Lista de credenciais do Usuário B não deve ser nula");
        assertEquals(2, listaB.size(), "Usuário B deve receber exatamente suas 2 credenciais");

        List<String> servicosB = listaB.stream().map(m -> (String) m.get("servico")).toList();
        assertTrue(servicosB.contains("Google"), "Usuário B deve ver a credencial Google");
        assertTrue(servicosB.contains("AWS"), "Usuário B deve ver a credencial AWS");
        assertFalse(servicosB.contains("GitHub"), "Usuário B NUNCA deve ver dados de outros usuários (GitHub)");
        assertFalse(servicosB.contains("ProtonMail"), "Usuário B NUNCA deve ver dados de outros usuários (ProtonMail)");
    }

    @Test
    @DisplayName("H07.3 - Dois usuários salvam credenciais concorrentemente e depois listam mantendo integridade e isolamento")
    void deveSalvarEListarCredenciaisConcorrentemente() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);

        // Submissão concorrente de escrita (POST) por ambos os usuários
        Callable<ResponseEntity<Map>> escritaUsuarioA = () -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return salvarCredencial(tokenA, "Slack", "userA@slack", "cipherSlack==", "ivS1");
        };

        Callable<ResponseEntity<Map>> escritaUsuarioB = () -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return salvarCredencial(tokenB, "Discord", "userB@discord", "cipherDiscord==", "ivD1");
        };

        Future<ResponseEntity<Map>> postFuturoA = executorService.submit(escritaUsuarioA);
        Future<ResponseEntity<Map>> postFuturoB = executorService.submit(escritaUsuarioB);

        startLatch.countDown();

        ResponseEntity<Map> postRespA = postFuturoA.get(10, TimeUnit.SECONDS);
        ResponseEntity<Map> postRespB = postFuturoB.get(10, TimeUnit.SECONDS);

        assertEquals(HttpStatus.CREATED, postRespA.getStatusCode());
        assertEquals(HttpStatus.CREATED, postRespB.getStatusCode());

        // Consulta subsequente para verificar isolamento imediato pós-escrita concorrente
        HttpEntity<Void> reqA = new HttpEntity<>(criarHeaders(tokenA));
        ResponseEntity<Map> getRespA = restTemplate.exchange(baseUrl, HttpMethod.GET, reqA, Map.class);
        assertEquals(HttpStatus.OK, getRespA.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listaA = (List<Map<String, Object>>) getRespA.getBody().get("credenciais");
        assertEquals(1, listaA.size());
        assertEquals("Slack", listaA.get(0).get("servico"));

        HttpEntity<Void> reqB = new HttpEntity<>(criarHeaders(tokenB));
        ResponseEntity<Map> getRespB = restTemplate.exchange(baseUrl, HttpMethod.GET, reqB, Map.class);
        assertEquals(HttpStatus.OK, getRespB.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listaB = (List<Map<String, Object>>) getRespB.getBody().get("credenciais");
        assertEquals(1, listaB.size());
        assertEquals("Discord", listaB.get(0).get("servico"));
    }

    // Métodos auxiliares
    private Long criarUsuario(String username) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, username);
            ps.setString(2, "hash_senha_mestra_123");
            ps.setString(3, "salt_aleatorio_456");
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private HttpHeaders criarHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<Map> salvarCredencial(String token, String servico, String usuarioServico, String dadosCifrados, String iv) {
        Map<String, String> body = Map.of(
                "servico", servico,
                "usuarioServico", usuarioServico,
                "dadosCifrados", dadosCifrados,
                "iv", iv
        );
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, criarHeaders(token));
        return restTemplate.postForEntity(baseUrl, request, Map.class);
    }
}
