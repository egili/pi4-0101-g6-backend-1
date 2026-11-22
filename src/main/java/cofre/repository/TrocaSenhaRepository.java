package cofre.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Troca a senha mestra e recifra todas as credenciais do usuário numa única transação. */
@Repository
public class TrocaSenhaRepository {

    public enum Resultado { OK, SENHA_ATUAL_INCORRETA, CREDENCIAIS_DIFERENTES }

    public record CredencialRecifrada(Long id, String dadosCifrados, String iv) {}

    private final JdbcTemplate jdbc;

    public TrocaSenhaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * shortcut: o conjunto de credenciais é conferido dentro da transação. Se o usuário criou ou apagou uma credencial
     * entre a leitura do Cliente e esta gravação, a troca é recusada, e o Cliente deve tentar de novo.
     */
    @Transactional
    public Resultado trocar(Long usuarioId, String hashAtual, String novoHash, String novoSalt, List<CredencialRecifrada> recifradas) {
        List<String> guardado = jdbc.queryForList(
                "SELECT senha_mestra_hash FROM usuario WHERE id = ? FOR UPDATE", String.class, usuarioId);
        if (guardado.isEmpty() || !iguais(guardado.get(0), hashAtual)) {
            return Resultado.SENHA_ATUAL_INCORRETA;
        }
        Set<Long> atuais = new HashSet<>(jdbc.queryForList("SELECT id FROM credencial WHERE usuario_id = ?", Long.class, usuarioId));
        Set<Long> enviadas = recifradas.stream().map(CredencialRecifrada::id).collect(Collectors.toSet());
        if (recifradas.size() != atuais.size() || !atuais.equals(enviadas)) {
            return Resultado.CREDENCIAIS_DIFERENTES;
        }
        jdbc.update("UPDATE usuario SET senha_mestra_hash = ?, salt = ? WHERE id = ?", novoHash, novoSalt, usuarioId);
        jdbc.batchUpdate(
                "UPDATE credencial SET dados_cifrados = ?, iv = ?, atualizado_em = CURRENT_TIMESTAMP WHERE id = ? AND usuario_id = ?",
                recifradas, recifradas.size(),
                (ps, c) -> {
                    ps.setString(1, c.dadosCifrados());
                    ps.setString(2, c.iv());
                    ps.setLong(3, c.id());
                    ps.setLong(4, usuarioId);
                });
        return Resultado.OK;
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
