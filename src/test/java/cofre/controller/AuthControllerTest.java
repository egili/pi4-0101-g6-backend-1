package cofre.controller;

import cofre.model.Usuario;
import cofre.repository.UsuarioRepository;
import cofre.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Nós "mockamos" (fingimos) o banco e o gerador de token
    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    public void deveRetornarSaltQuandoUsuarioExiste() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("eliseu");
        usuario.setSalt("salt-de-teste");

        // Finge que o banco achou o usuário
        when(usuarioRepository.findByUsername("eliseu")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/salt/eliseu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salt").value("salt-de-teste"));
    }

    @Test
    public void deveRetornar404QuandoUsuarioNaoExisteNoSalt() throws Exception {
        // Finge que o banco não achou ninguém
        when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        mockMvc.perform(get("/salt/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Usuário não encontrado"));
    }

    @Test
    public void deveFazerLoginERetornarToken() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("eliseu");
        usuario.setSenhaMestraHash("hash-falso");

        when(usuarioRepository.findByUsername("eliseu")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(anyString())).thenReturn("token-jwt-falso");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"eliseu\", \"hashLogin\": \"hash-falso\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-falso"))
                .andExpect(jsonPath("$.usuarioId").value(1));
    }

    @Test
    public void deveRetornar401ComSenhaErrada() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("eliseu");
        usuario.setSenhaMestraHash("hash-correto");

        when(usuarioRepository.findByUsername("eliseu")).thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"eliseu\", \"hashLogin\": \"hash-errado\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Usuário ou senha inválidos"));
    }
}