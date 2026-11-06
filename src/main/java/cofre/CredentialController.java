package cofre;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class CredentialController {

    private final CredentialService credentialService;
    private final JwtService jwtService;

    public CredentialController(CredentialService credentialService, JwtService jwtService) {
        this.credentialService = credentialService;
        this.jwtService = jwtService;
    }

    @PostMapping({"/credentials", "/api/credentials"})
    public ResponseEntity<?> salvarCredencial(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "tokenSessao", required = false) String tokenSessaoHeader,
            @RequestBody(required = false) CredentialRequest req) {

        // Extrai e valida o token JWT do header Authorization ou tokenSessao
        String token = authHeader != null && !authHeader.isBlank() ? authHeader : tokenSessaoHeader;
        Optional<Long> usuarioIdOpt = jwtService.extrairUsuarioId(token);

        if (usuarioIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "SESSAO_INVALIDA"));
        }

        // Valida se os campos obrigatórios foram fornecidos
        if (req == null || req.isInvalido()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "DADOS_INVALIDOS"));
        }

        try {
            CredentialResponse response = credentialService.salvarCredencial(usuarioIdOpt.get(), req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UsuarioNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "USUARIO_NAO_ENCONTRADO"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "DADOS_INVALIDOS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "ERRO_BANCO"));
        }
    }

    @GetMapping({"/credentials", "/api/credentials"})
    public ResponseEntity<?> listarCredenciais(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "tokenSessao", required = false) String tokenSessaoHeader) {

        String token = authHeader != null && !authHeader.isBlank() ? authHeader : tokenSessaoHeader;
        Optional<Long> usuarioIdOpt = jwtService.extrairUsuarioId(token);

        if (usuarioIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "SESSAO_INVALIDA"));
        }

        try {
            return ResponseEntity.ok(credentialService.listarCredenciais(usuarioIdOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "ERRO_BANCO"));
        }
    }
}

