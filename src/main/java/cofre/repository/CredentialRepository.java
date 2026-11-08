package cofre.repository;

import cofre.model.CredencialItem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class CredentialRepository {

    private final JdbcTemplate jdbc;

    public CredentialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean usuarioExiste(Long usuarioId) {
        if (usuarioId == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE id = ?",
                Integer.class,
                usuarioId
        );
        return count != null && count > 0;
    }

    public Long salvar(Long usuarioId, String servico, String usuarioServico, String dadosCifrados, String iv) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO credencial (usuario_id, servico, usuario_servico, dados_cifrados, iv) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, usuarioId);
            ps.setString(2, servico);
            ps.setString(3, usuarioServico);
            ps.setString(4, dadosCifrados);
            ps.setString(5, iv);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new RuntimeException("Falha ao obter ID gerado para a credencial");
        }
        return key.longValue();
    }

    public List<CredencialItem> listarPorUsuario(Long usuarioId) {
        return jdbc.query(
                "SELECT id, servico, usuario_servico, dados_cifrados, iv FROM credencial WHERE usuario_id = ? ORDER BY id",
                (rs, n) -> new CredencialItem(
                        rs.getLong("id"),
                        rs.getString("servico"),
                        rs.getString("usuario_servico"),
                        rs.getString("dados_cifrados"),
                        rs.getString("iv")
                ),
                usuarioId
        );
    }

    public Optional<Long> buscarUsuarioIdPorCredencialId(Long credencialId) {
        try {
            Long usuarioId = jdbc.queryForObject(
                    "SELECT usuario_id FROM credencial WHERE id = ?",
                    Long.class,
                    credencialId
            );
            return Optional.ofNullable(usuarioId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    public void atualizar(Long id, String servico, String usuarioServico, String dadosCifrados, String iv) {
        jdbc.update(
                "UPDATE credencial SET servico = ?, usuario_servico = ?, dados_cifrados = ?, iv = ? WHERE id = ?",
                servico, usuarioServico, dadosCifrados, iv, id
        );
    }

    public void deletar(Long id) {
        jdbc.update("DELETE FROM credencial WHERE id = ?", id);
    }
}