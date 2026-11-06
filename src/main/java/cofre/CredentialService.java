package cofre;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CredentialService {

    private final CredentialRepository repository;

    public CredentialService(CredentialRepository repository) {
        this.repository = repository;
    }

    public CredentialResponse salvarCredencial(Long usuarioId, CredentialRequest req) {
        if (req == null || req.isInvalido()) {
            throw new IllegalArgumentException("DADOS_INVALIDOS");
        }

        if (usuarioId == null || !repository.usuarioExiste(usuarioId)) {
            throw new UsuarioNaoEncontradoException("USUARIO_NAO_ENCONTRADO");
        }

        try {
            Long id = repository.salvar(
                    usuarioId,
                    req.servico(),
                    req.usuarioServico(),
                    req.dadosCifrados(),
                    req.iv()
            );
            return new CredentialResponse(id, "Credencial salva com sucesso");
        } catch (DataIntegrityViolationException e) {
            throw new UsuarioNaoEncontradoException("USUARIO_NAO_ENCONTRADO");
        }
    }

    public List<CredencialItem> listarCredenciais(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("SESSAO_INVALIDA");
        }
        return repository.listarPorUsuario(usuarioId);
    }
}

