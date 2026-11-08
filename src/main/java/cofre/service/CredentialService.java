package cofre.service;

import cofre.CredentialRequest;
import cofre.CredentialResponse;
import cofre.model.CredencialItem;
import cofre.UsuarioNaoEncontradoException;
import cofre.repository.CredentialRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    // ROTAS PUT E DELETE

    public CredentialResponse editarCredencial(Long credencialId, Long usuarioIdLogado, CredentialRequest req) {
        if (req == null || req.isInvalido()) {
            throw new IllegalArgumentException("DADOS_INVALIDOS");
        }

        // 1. Busca o dono da credencial no banco
        Optional<Long> donoId = repository.buscarUsuarioIdPorCredencialId(credencialId);

        // Se não achou a credencial, lança exceção (Controller transforma em 404)
        if (donoId.isEmpty()) {
            throw new UsuarioNaoEncontradoException("CREDENCIAL_NAO_ENCONTRADA");
        }

        // 2. VALIDAÇÃO DE DONO: Se o dono for diferente de quem está logado, bloqueia!
        if (!donoId.get().equals(usuarioIdLogado)) {
            throw new SecurityException("ACESSO_NEGADO"); // Controller transforma em 403
        }

        // 3. Atualiza no banco
        try {
            repository.atualizar(
                    credencialId,
                    req.servico(),
                    req.usuarioServico(),
                    req.dadosCifrados(),
                    req.iv()
            );
            return new CredentialResponse(credencialId, "Credencial atualizada com sucesso");
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("DADOS_INVALIDOS");
        }
    }

    public void excluirCredencial(Long credencialId, Long usuarioIdLogado) {
        // 1. Busca o dono da credencial
        Optional<Long> donoId = repository.buscarUsuarioIdPorCredencialId(credencialId);

        if (donoId.isEmpty()) {
            throw new UsuarioNaoEncontradoException("CREDENCIAL_NAO_ENCONTRADA");
        }

        // 2. VALIDAÇÃO DE DONO
        if (!donoId.get().equals(usuarioIdLogado)) {
            throw new SecurityException("ACESSO_NEGADO");
        }

        // 3. Exclui do banco
        repository.deletar(credencialId);
    }
}