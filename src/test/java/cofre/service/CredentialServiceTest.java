package cofre.service;

import cofre.CredentialRequest;
import cofre.CredentialResponse;
import cofre.UsuarioNaoEncontradoException;
import cofre.repository.CredentialRepository;
import cofre.model.CredencialItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    private CredentialRepository repository;

    @InjectMocks
    private CredentialService credentialService;

    @Test
    @DisplayName("Deve salvar credencial com sucesso quando a requisição for válida e o usuário existir")
    void deveSalvarCredencialComSucessoQuandoDadosValidosEUsuarioExiste() {
        Long usuarioId = 1L;
        Long credencialIdGerado = 100L;
        CredentialRequest request = new CredentialRequest(
                "Netflix",
                "usuario@teste.com",
                "dadosCifradosBase64==",
                "iv1234567890"
        );

        when(repository.usuarioExiste(usuarioId)).thenReturn(true);
        when(repository.salvar(
                usuarioId,
                request.servico(),
                request.usuarioServico(),
                request.dadosCifrados(),
                request.iv()
        )).thenReturn(credencialIdGerado);

        CredentialResponse response = credentialService.salvarCredencial(usuarioId, request);

        assertNotNull(response);
        assertEquals(credencialIdGerado, response.credencialId());
        assertEquals("Credencial salva com sucesso", response.mensagem());

        verify(repository, times(1)).usuarioExiste(usuarioId);
        verify(repository, times(1)).salvar(
                usuarioId,
                "Netflix",
                "usuario@teste.com",
                "dadosCifradosBase64==",
                "iv1234567890"
        );
    }

    @Test
    @DisplayName("Deve lançar UsuarioNaoEncontradoException quando o usuário não existir no repositório")
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        Long usuarioIdInexistente = 999L;
        CredentialRequest request = new CredentialRequest(
                "Github",
                "dev",
                "cifrado==",
                "iv123"
        );

        when(repository.usuarioExiste(usuarioIdInexistente)).thenReturn(false);

        UsuarioNaoEncontradoException exception = assertThrows(
                UsuarioNaoEncontradoException.class,
                () -> credentialService.salvarCredencial(usuarioIdInexistente, request)
        );

        assertEquals("USUARIO_NAO_ENCONTRADO", exception.getMessage());
        verify(repository, times(1)).usuarioExiste(usuarioIdInexistente);
        verify(repository, never()).salvar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar UsuarioNaoEncontradoException quando o usuarioId for nulo")
    void deveLancarExcecaoQuandoUsuarioIdForNulo() {
        CredentialRequest request = new CredentialRequest(
                "Github",
                "dev",
                "cifrado==",
                "iv123"
        );

        UsuarioNaoEncontradoException exception = assertThrows(
                UsuarioNaoEncontradoException.class,
                () -> credentialService.salvarCredencial(null, request)
        );

        assertEquals("USUARIO_NAO_ENCONTRADO", exception.getMessage());
        verify(repository, never()).usuarioExiste(any());
        verify(repository, never()).salvar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar UsuarioNaoEncontradoException quando o repositório lançar DataIntegrityViolationException")
    void deveLancarExcecaoQuandoOcorrerViolacaoDeIntegridade() {
        Long usuarioId = 1L;
        CredentialRequest request = new CredentialRequest(
                "Spotify",
                "user",
                "cifrado==",
                "iv123"
        );

        when(repository.usuarioExiste(usuarioId)).thenReturn(true);
        when(repository.salvar(any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("Violação de FK"));

        UsuarioNaoEncontradoException exception = assertThrows(
                UsuarioNaoEncontradoException.class,
                () -> credentialService.salvarCredencial(usuarioId, request)
        );

        assertEquals("USUARIO_NAO_ENCONTRADO", exception.getMessage());
        verify(repository, times(1)).usuarioExiste(usuarioId);
        verify(repository, times(1)).salvar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a requisição for nula")
    void deveLancarExcecaoQuandoRequisicaoForNula() {
        Long usuarioId = 1L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.salvarCredencial(usuarioId, null)
        );

        assertEquals("DADOS_INVALIDOS", exception.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest(name = "Cenário inválido {index}: servico={0}, usuarioServico={1}, dadosCifrados={2}, iv={3}")
    @MethodSource("requisicoesInvalidasProvider")
    @DisplayName("Deve lançar IllegalArgumentException quando algum campo obrigatório for nulo ou vazio")
    void deveLancarExcecaoQuandoCamposForemInvalidos(String servico, String usuarioServico, String dadosCifrados, String iv) {
        Long usuarioId = 1L;
        CredentialRequest request = new CredentialRequest(servico, usuarioServico, dadosCifrados, iv);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.salvarCredencial(usuarioId, request)
        );

        assertEquals("DADOS_INVALIDOS", exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("listarCredenciais devolve a lista cifrada do repositório")
    void listaCredenciaisDoUsuario() {
        Long usuarioId = 1L;
        List<CredencialItem> armazenadas = List.of(
                new CredencialItem(10L, "Gmail", "eu@gmail.com", "blob", "iv")
        );
        when(repository.listarPorUsuario(usuarioId)).thenReturn(armazenadas);

        List<CredencialItem> lista = credentialService.listarCredenciais(usuarioId);

        assertEquals(armazenadas, lista);
        verify(repository, times(1)).listarPorUsuario(usuarioId);
    }

    @Test
    @DisplayName("listarCredenciais recusa usuarioId nulo")
    void listarRecusaUsuarioNulo() {
        assertThrows(IllegalArgumentException.class, () -> credentialService.listarCredenciais(null));
        verify(repository, never()).listarPorUsuario(any());
    }

    private static Stream<Arguments> requisicoesInvalidasProvider() {
        return Stream.of(
                Arguments.of(null, "usuario", "cifrado", "iv"),
                Arguments.of("", "usuario", "cifrado", "iv"),
                Arguments.of("   ", "usuario", "cifrado", "iv"),
                Arguments.of("servico", null, "cifrado", "iv"),
                Arguments.of("servico", "", "cifrado", "iv"),
                Arguments.of("servico", "   ", "cifrado", "iv"),
                Arguments.of("servico", "usuario", null, "iv"),
                Arguments.of("servico", "usuario", "", "iv"),
                Arguments.of("servico", "usuario", "   ", "iv"),
                Arguments.of("servico", "usuario", "cifrado", null),
                Arguments.of("servico", "usuario", "cifrado", ""),
                Arguments.of("servico", "usuario", "cifrado", "   ")
        );
    }
}
