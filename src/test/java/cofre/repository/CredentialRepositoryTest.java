package cofre.repository;

import cofre.model.CredencialItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CredentialRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CredentialRepository repository;
    private Long usuarioIdValido;

    @BeforeEach
    void setUp() {
        repository = new CredentialRepository(jdbcTemplate);

        String username = "usuario.repo." + System.nanoTime();
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, username);
            ps.setString(2, "hashSenha");
            ps.setString(3, "saltAleatorio");
            return ps;
        }, chave);

        usuarioIdValido = chave.getKey().longValue();
    }

    @Test
    @DisplayName("usuarioExiste deve retornar true quando o usuário existir no banco")
    void deveRetornarTrueQuandoUsuarioExiste() {
        boolean existe = repository.usuarioExiste(usuarioIdValido);
        assertTrue(existe);
    }

    @Test
    @DisplayName("usuarioExiste deve retornar false quando o usuário não existir no banco")
    void deveRetornarFalseQuandoUsuarioNaoExiste() {
        boolean existe = repository.usuarioExiste(999999L);
        assertFalse(existe);
    }

    @Test
    @DisplayName("usuarioExiste deve retornar false quando o id for nulo")
    void deveRetornarFalseQuandoUsuarioIdForNulo() {
        boolean existe = repository.usuarioExiste(null);
        assertFalse(existe);
    }

    @Test
    @DisplayName("salvar deve persistir a credencial cifrada e retornar o ID gerado")
    void deveSalvarCredencialERetornarId() {
        String servico = "ProtonMail";
        String usuarioServico = "privado@proton.me";
        String dadosCifrados = "cipherBytesBase64==";
        String iv = "ivVector123";

        Long credencialId = repository.salvar(usuarioIdValido, servico, usuarioServico, dadosCifrados, iv);

        assertNotNull(credencialId);
        assertTrue(credencialId > 0);

        Map<String, Object> linha = jdbcTemplate.queryForMap(
                "SELECT * FROM credencial WHERE id = ?",
                credencialId
        );

        assertEquals(usuarioIdValido.intValue(), ((Number) linha.get("usuario_id")).intValue());
        assertEquals(servico, linha.get("servico"));
        assertEquals(usuarioServico, linha.get("usuario_servico"));
        assertEquals(dadosCifrados, linha.get("dados_cifrados"));
        assertEquals(iv, linha.get("iv"));
    }

    @Test
    @DisplayName("buscarUsuarioIdPorCredencialId deve retornar o usuarioId correspondente quando a credencial existir")
    void deveBuscarUsuarioIdPorCredencialIdQuandoExistir() {
        Long credencialId = repository.salvar(
                usuarioIdValido,
                "AWS",
                "admin",
                "dadosEnc==",
                "iv456"
        );

        Optional<Long> usuarioIdEncontrado = repository.buscarUsuarioIdPorCredencialId(credencialId);

        assertTrue(usuarioIdEncontrado.isPresent());
        assertEquals(usuarioIdValido, usuarioIdEncontrado.get());
    }

    @Test
    @DisplayName("buscarUsuarioIdPorCredencialId deve retornar Optional.empty() quando a credencial não existir")
    void deveRetornarEmptyQuandoCredencialNaoExistir() {
        Optional<Long> usuarioId = repository.buscarUsuarioIdPorCredencialId(999999L);
        assertTrue(usuarioId.isEmpty());
    }

    @Test
    @DisplayName("listarPorUsuario devolve só os blobs cifrados daquele usuario_id")
    void listaSomenteCredenciaisDoUsuario() {
        repository.salvar(usuarioIdValido, "Gmail", "eu@gmail.com", "blob-meu", "iv-meu");

        KeyHolder outro = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, "outro.repo." + System.nanoTime());
            ps.setString(2, "hashSenha");
            ps.setString(3, "saltAleatorio");
            return ps;
        }, outro);
        repository.salvar(outro.getKey().longValue(), "Banco", "outro@banco.com", "blob-alheio", "iv-alheio");

        List<CredencialItem> lista = repository.listarPorUsuario(usuarioIdValido);

        assertEquals(1, lista.size());
        CredencialItem item = lista.get(0);
        assertEquals("Gmail", item.servico());
        assertEquals("eu@gmail.com", item.usuarioServico());
        assertEquals("blob-meu", item.dadosCifrados());
        assertEquals("iv-meu", item.iv());
        assertNotNull(item.credencialId());
    }
}
