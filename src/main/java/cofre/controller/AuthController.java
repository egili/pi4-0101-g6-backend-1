package cofre.controller;

import cofre.model.Usuario;
import cofre.repository.UsuarioRepository;
import cofre.service.JwtService;
import cofre.service.LimiteLogin;
import cofre.service.SaltFalso;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SaltFalso saltFalso;

    @Autowired
    private LimiteLogin limiteLogin;

    // 1. GET /salt/{user} - O Front busca o salt ANTES de fazer o login.
    // Usuário inexistente recebe um salt falso e estável, então a resposta não revela quem tem conta.
    @GetMapping("/salt/{user}")
    public ResponseEntity<?> getSalt(@PathVariable String user) {
        Optional<Usuario> usuario = usuarioRepository.findByUsername(user);
        String salt = usuario.map(Usuario::getSalt).orElseGet(() -> saltFalso.gerar(user));
        return ResponseEntity.ok(Map.of("salt", salt));
    }

    // 2. POST /login - O Front manda o hash e recebe o Token JWT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String hashLogin = body.get("hashLogin");
        String chaveUsuario = "usuario:" + username;
        String chaveIp = "ip:" + request.getRemoteAddr();

        if (limiteLogin.bloqueado(chaveUsuario, LimiteLogin.MAX_FALHAS_USUARIO)
                || limiteLogin.bloqueado(chaveIp, LimiteLogin.MAX_FALHAS_IP)) {
            return ResponseEntity.status(429)
                    .body(Map.of("erro", "Muitas tentativas de login. Aguarde 15 minutos e tente de novo."));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isEmpty() || hashLogin == null || !MessageDigest.isEqual(
                usuarioOpt.get().getSenhaMestraHash().getBytes(StandardCharsets.UTF_8),
                hashLogin.getBytes(StandardCharsets.UTF_8))) {
            limiteLogin.registrarFalha(chaveUsuario);
            limiteLogin.registrarFalha(chaveIp);
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário ou senha inválidos"));
        }

        Usuario usuario = usuarioOpt.get();
        limiteLogin.limpar(chaveUsuario);

        // Mesmo formato de token que as rotas de credenciais e de backup validam (usuarioId no payload)
        String token = jwtService.gerarToken(usuario.getId(), usuario.getUsername());

        return ResponseEntity.ok(Map.of(
            "token", token,
            "usuarioId", usuario.getId()
        ));
    }
}
