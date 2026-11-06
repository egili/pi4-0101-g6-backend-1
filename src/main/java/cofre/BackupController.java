package cofre;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class BackupController {

    private final BackupRepository repository;
    private final JwtService jwtService;

    public BackupController(BackupRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @PostMapping("/backup")
    public ResponseEntity<?> importarBackup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) List<CredentialRequest> credenciais) {

        Optional<Long> usuarioIdOpt = jwtService.extrairUsuarioId(authHeader);
        if (usuarioIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "SESSAO_INVALIDA"));
        }

        if (credenciais == null || credenciais.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "BACKUP_VAZIO"));
        }

        if (credenciais.stream().anyMatch(c -> c == null || c.isInvalido())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "DADOS_INVALIDOS"));
        }

        try {
            repository.inserirCredenciais(usuarioIdOpt.get(), credenciais);
            return ResponseEntity.ok(Map.of(
                    "credenciaisInseridas", credenciais.size(),
                    "mensagem", "Backup restaurado com sucesso"
            ));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "ERRO_BANCO"));
        }
    }

    @GetMapping("/backup")
    public ResponseEntity<?> exportarBackup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        Optional<Long> usuarioIdOpt = jwtService.extrairUsuarioId(authHeader);
        if (usuarioIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "SESSAO_INVALIDA"));
        }

        try {
            Optional<BackupRepository.UsuarioBackup> usuario = repository.buscarUsuario(usuarioIdOpt.get());
            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "USUARIO_NAO_ENCONTRADO"));
            }

            List<BackupRepository.CredencialBackup> credenciais = repository.listarCredenciais(usuarioIdOpt.get());
            if (credenciais.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "BACKUP_VAZIO"));
            }

            return ResponseEntity.ok(Map.of(
                    "credenciais", credenciais,
                    "usuario", usuario.get(),
                    "mensagem", "Backup pronto para exportação"
            ));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "ERRO_BANCO"));
        }
    }
}
