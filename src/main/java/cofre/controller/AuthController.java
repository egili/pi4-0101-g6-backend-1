package cofre.controller;

import cofre.model.Usuario;
import cofre.repository.UsuarioRepository;
import cofre.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    // 1. GET /salt/{user} - O Front busca o salt ANTES de fazer o login
    @GetMapping("/salt/{user}")
    public ResponseEntity<?> getSalt(@PathVariable String user) {
        Optional<Usuario> usuario = usuarioRepository.findByUsername(user);
        
        if (usuario.isPresent()) {
            return ResponseEntity.ok(Map.of("salt", usuario.get().getSalt()));
        }
        return ResponseEntity.status(404).body(Map.of("erro", "Usuário não encontrado"));
    }

    // 2. POST /login - O Front manda o hash e recebe o Token JWT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String hashLogin = body.get("hashLogin");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário ou senha inválidos"));
        }

        Usuario usuario = usuarioOpt.get();

        // Compara o hash enviado com o hash salvo no banco
        if (!usuario.getSenhaMestraHash().equals(hashLogin)) {
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário ou senha inválidos"));
        }

        // Gera o Token
        // Mesmo formato de token que as rotas de credenciais e de backup validam (usuarioId no payload)
        String token = jwtService.gerarToken(usuario.getId(), usuario.getUsername());

        return ResponseEntity.ok(Map.of(
            "token", token,
            "usuarioId", usuario.getId()
        ));
    }
}