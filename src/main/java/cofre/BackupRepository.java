package cofre;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BackupRepository {

    private final JdbcTemplate jdbc;

    public BackupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UsuarioBackup> buscarUsuario(Long usuarioId) {
        return jdbc.query(
                "SELECT id, username, senha_mestra_hash, salt FROM usuario WHERE id = ?",
                (rs, n) -> new UsuarioBackup(rs.getLong("id"), rs.getString("username"),
                        rs.getString("senha_mestra_hash"), rs.getString("salt")),
                usuarioId
        ).stream().findFirst();
    }

    public List<CredencialBackup> listarCredenciais(Long usuarioId) {
        return jdbc.query(
                "SELECT id, servico, usuario_servico, dados_cifrados, iv FROM credencial WHERE usuario_id = ? ORDER BY id",
                (rs, n) -> new CredencialBackup(rs.getLong("id"), rs.getString("servico"),
                        rs.getString("usuario_servico"), rs.getString("dados_cifrados"), rs.getString("iv")),
                usuarioId
        );
    }

    public record UsuarioBackup(Long usuarioId, String username, String senhaMestraHash, String salt) {}

    public record CredencialBackup(Long credencialId, String servico, String usuarioServico, String dadosCifrados, String iv) {}
}
