package cofre.controller;

import cofre.model.Usuario;
import cofre.repository.UsuarioRepository;
import cofre.service.JwtService;
import cofre.service.LimiteLogin;
import cofre.service.SaltFalso;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SaltFalso.class, LimiteLogin.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaltFalso saltFalso;

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
    public void usuarioInexistenteRecebeSaltFalsoEstavel() throws Exception {
        when(usuarioRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

        mockMvc.perform(get("/salt/ninguem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salt").value(saltFalso.gerar("ninguem")));
    }

    @Test
    public void saltFalsoNaoDistingueContaInexistenteDeExistente() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("existe");
        usuario.setSalt("c2FsdC1yZWFs");
        when(usuarioRepository.findByUsername("existe")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByUsername("nao-existe")).thenReturn(Optional.empty());

        String comConta = mockMvc.perform(get("/salt/existe")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String semConta = mockMvc.perform(get("/salt/nao-existe")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotEquals(comConta, semConta);
        assertEquals(saltFalso.gerar("nao-existe"), saltFalso.gerar("nao-existe"));
    }

    @Test
    public void deveFazerLoginERetornarToken() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("eliseu");
        usuario.setSenhaMestraHash("hash-falso");

        when(usuarioRepository.findByUsername("eliseu")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(anyLong(), anyString())).thenReturn("token-jwt-falso");

        login("eliseu", "hash-falso", "10.0.0.1")
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

        login("eliseu", "hash-errado", "10.0.0.2")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Usuário ou senha inválidos"));
    }

    @Test
    public void bloqueiaUsuarioAposCincoFalhasMesmoComSenhaCerta() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setUsername("bloqueado");
        usuario.setSenhaMestraHash("hash-certo");
        when(usuarioRepository.findByUsername("bloqueado")).thenReturn(Optional.of(usuario));

        for (int i = 0; i < LimiteLogin.MAX_FALHAS_USUARIO; i++) {
            login("bloqueado", "errada", "10.0.1." + i).andExpect(status().isUnauthorized());
        }

        login("bloqueado", "hash-certo", "10.0.2.1").andExpect(status().isTooManyRequests());
    }

    @Test
    public void bloqueiaIpAposFalhasDeVariosUsuarios() throws Exception {
        for (int i = 0; i < LimiteLogin.MAX_FALHAS_IP; i++) {
            login("inexistente" + i, "x", "10.9.9.9").andExpect(status().isUnauthorized());
        }

        login("qualquer", "x", "10.9.9.9").andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String username, String hash, String ip) throws Exception {
        return mockMvc.perform(post("/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"hashLogin\": \"" + hash + "\"}"));
    }
}
