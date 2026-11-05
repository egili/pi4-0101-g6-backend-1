package cofre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SEGREDO_TESTE = "chave-secreta-de-testes-unitarios-jwt-segura-256-bits";
    private static final long EXPIRACAO_SEGUNDOS = 3600;

    private JwtService jwtService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtService = new JwtService(SEGREDO_TESTE, EXPIRACAO_SEGUNDOS, objectMapper);
    }

    @Test
    @DisplayName("Deve gerar token JWT válido com 3 partes separadas por ponto")
    void deveGerarTokenComEstruturaValida() {
        Long usuarioId = 42L;
        String username = "usuario.unit";

        String token = jwtService.gerarToken(usuarioId, username);

        assertNotNull(token);
        String[] partes = token.split("\\.");
        assertEquals(3, partes.length, "O token JWT deve conter exatamente 3 partes (header.payload.signature)");
    }

    @Test
    @DisplayName("Deve extrair usuarioId com sucesso a partir de token puro")
    void deveExtrairUsuarioIdDeTokenPuro() {
        Long usuarioIdEsperado = 123L;
        String token = jwtService.gerarToken(usuarioIdEsperado, "maria");

        Optional<Long> usuarioIdExtraido = jwtService.extrairUsuarioId(token);

        assertTrue(usuarioIdExtraido.isPresent());
        assertEquals(usuarioIdEsperado, usuarioIdExtraido.get());
        assertTrue(jwtService.isTokenValido(token));
    }

    @Test
    @DisplayName("Deve extrair usuarioId com sucesso quando o token contiver o prefixo 'Bearer '")
    void deveExtrairUsuarioIdComPrefixoBearer() {
        Long usuarioIdEsperado = 456L;
        String token = jwtService.gerarToken(usuarioIdEsperado, "joao");
        String headerAuthorization = "Bearer " + token;

        Optional<Long> usuarioIdExtraido = jwtService.extrairUsuarioId(headerAuthorization);

        assertTrue(usuarioIdExtraido.isPresent());
        assertEquals(usuarioIdEsperado, usuarioIdExtraido.get());
        assertTrue(jwtService.isTokenValido(headerAuthorization));
    }

    @Test
    @DisplayName("Deve retornar vazio ao tentar extrair usuarioId de token com assinatura adulterada")
    void deveRecusarTokenComAssinaturaAdulterada() {
        String tokenValido = jwtService.gerarToken(789L, "alberto");
        String[] partes = tokenValido.split("\\.");
        
        // Adultera a assinatura (terceira parte)
        String tokenAdulterado = partes[0] + "." + partes[1] + ".assinaturaFalsa123";

        Optional<Long> usuarioId = jwtService.extrairUsuarioId(tokenAdulterado);

        assertTrue(usuarioId.isEmpty());
        assertFalse(jwtService.isTokenValido(tokenAdulterado));
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token estiver expirado")
    void deveRecusarTokenExpirado() {
        // JwtService com expiração negativa (já expirado imediatamente)
        JwtService jwtServiceExpirado = new JwtService(SEGREDO_TESTE, -60, objectMapper);
        String tokenExpirado = jwtServiceExpirado.gerarToken(99L, "expirado");

        Optional<Long> usuarioId = jwtServiceExpirado.extrairUsuarioId(tokenExpirado);

        assertTrue(usuarioId.isEmpty());
        assertFalse(jwtServiceExpirado.isTokenValido(tokenExpirado));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "tokenInvalidoSemPontos", "parte1.parte2", "Bearer ", "Bearer tokenSemFormato"})
    @DisplayName("Deve retornar vazio para entradas nulas, vazias ou malformadas")
    void deveRetornarVazioParaTokensInvalidosOuVazios(String tokenInvalido) {
        Optional<Long> resultado = jwtService.extrairUsuarioId(tokenInvalido);

        assertTrue(resultado.isEmpty());
        assertFalse(jwtService.isTokenValido(tokenInvalido));
    }
}
