package cofre;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.sql.PreparedStatement;
import java.util.Map;

@RestController
public class UsuarioController {

    private final JdbcTemplate jdbc;

    public UsuarioController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> cadastrar(@RequestBody CadastroRequest req) {
        if (vazio(req.username()) || vazio(req.senhaMestraHash()) || vazio(req.salt())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "CAMPO_OBRIGATORIO_AUSENTE"));
        }
        KeyHolder chave = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)", new String[]{"id"});
                ps.setString(1, req.username());
                ps.setString(2, req.senhaMestraHash());
                ps.setString(3, req.salt());
                return ps;
            }, chave);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("usuarioId", chave.getKey().longValue(), "mensagem", "Conta criada com sucesso"));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", "USUARIO_JA_EXISTE"));
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public record CadastroRequest(String username, String senhaMestraHash, String salt) {}
}
