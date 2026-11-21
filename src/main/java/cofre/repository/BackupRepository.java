package cofre.repository;

import cofre.dto.CredentialRequest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void inserirCredenciais(Long usuarioId, List<CredentialRequest> credenciais) {
        jdbc.batchUpdate(
                "INSERT INTO credencial (usuario_id, servico, usuario_servico, dados_cifrados, iv) VALUES (?, ?, ?, ?, ?)",
                credenciais,
                credenciais.size(),
                (ps, c) -> {
                    ps.setLong(1, usuarioId);
                    ps.setString(2, c.servico());
                    ps.setString(3, c.usuarioServico());
                    ps.setString(4, c.dadosCifrados());
                    ps.setString(5, c.iv());
                }
        );
    }

    public record UsuarioBackup(Long usuarioId, String username, String senhaMestraHash, String salt) {}

}
