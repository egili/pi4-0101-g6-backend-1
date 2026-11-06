package cofre.controller;

import cofre.CredentialRequest;
import cofre.CredentialResponse;
import cofre.service.CredentialService;
import cofre.service.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CredentialController.class)
public class CredentialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private JwtService jwtService;

    private final String jsonPayload = "{\"servico\": \"Gmail\", \"usuarioServico\": \"leo\", \"dadosCifrados\": \"abc\", \"iv\": \"123\"}";

    @Test
    public void deveEditarCredencialComSucesso() throws Exception {
        when(jwtService.extrairUsuarioId(anyString())).thenReturn(Optional.of(1L));
        
        when(credentialService.editarCredencial(eq(10L), eq(1L), any(CredentialRequest.class)))
                .thenReturn(new CredentialResponse(10L, "Credencial atualizada com sucesso"));

        mockMvc.perform(put("/credentials/10")
                .header("Authorization", "Bearer token-valido")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                // Verifica se a mensagem veio correta
                .andExpect(jsonPath("$.mensagem").value("Credencial atualizada com sucesso"))
                // Verifica se o ID também foi retornado no JSON
                .andExpect(jsonPath("$.credencialId").value(10));
    }

    @Test
    public void deveRetornar403AoTentarEditarCredencialDeOutroUsuario() throws Exception {
        when(jwtService.extrairUsuarioId(anyString())).thenReturn(Optional.of(1L));
        
        when(credentialService.editarCredencial(eq(10L), eq(1L), any(CredentialRequest.class)))
                .thenThrow(new SecurityException("ACESSO_NEGADO"));

        mockMvc.perform(put("/credentials/10")
                .header("Authorization", "Bearer token-valido")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));
    }

    @Test
    public void deveRetornar401AoTentarEditarSemTokenValido() throws Exception {
        when(jwtService.extrairUsuarioId(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(put("/credentials/10")
                .header("Authorization", "Bearer token-invalido")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("SESSAO_INVALIDA"));
    }

    @Test
    public void deveExcluirCredencialComSucesso() throws Exception {
        when(jwtService.extrairUsuarioId(anyString())).thenReturn(Optional.of(1L));
        
        doNothing().when(credentialService).excluirCredencial(eq(10L), eq(1L));

        mockMvc.perform(delete("/credentials/10")
                .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Credencial excluída com sucesso"));
    }

    @Test
    public void deveRetornar403AoTentarExcluirCredencialDeOutroUsuario() throws Exception {
        when(jwtService.extrairUsuarioId(anyString())).thenReturn(Optional.of(1L));
        
        doThrow(new SecurityException("ACESSO_NEGADO"))
                .when(credentialService).excluirCredencial(eq(10L), eq(1L));

        mockMvc.perform(delete("/credentials/10")
                .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));
    }
}