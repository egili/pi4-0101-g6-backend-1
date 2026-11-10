package cofre.service;

import cofre.BackupRepository;
import cofre.CredentialRequest;
import cofre.CredentialResponse;
import cofre.UsuarioNaoEncontradoException;
import cofre.model.CredencialItem;
import cofre.model.Usuario;
import cofre.protocolo.Comunicado;
import cofre.protocolo.CredencialDto;
import cofre.protocolo.PedidoDeAtualizacao;
import cofre.protocolo.PedidoDeCadastro;
import cofre.protocolo.PedidoDeCriacao;
import cofre.protocolo.PedidoDeExclusao;
import cofre.protocolo.PedidoDeExportacao;
import cofre.protocolo.PedidoDeImportacao;
import cofre.protocolo.PedidoDeListagem;
import cofre.protocolo.PedidoDeLogin;
import cofre.protocolo.PedidoDeSalt;
import cofre.protocolo.Resposta;
import cofre.repository.UsuarioRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Regra de negócio do Servidor. Atende cada pedido recebido por socket e devolve uma Resposta. */
@Service
public class NegocioServidor {

    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final SaltFalso saltFalso;
    private final LimiteLogin limiteLogin;
    private final JwtService jwtService;
    private final CredentialService credenciais;
    private final BackupRepository backup;

    public NegocioServidor(UsuarioRepository usuarios, JdbcTemplate jdbc, SaltFalso saltFalso, LimiteLogin limiteLogin,
                           JwtService jwtService, CredentialService credenciais, BackupRepository backup) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.saltFalso = saltFalso;
        this.limiteLogin = limiteLogin;
        this.jwtService = jwtService;
        this.credenciais = credenciais;
        this.backup = backup;
    }

    public Resposta processar(Comunicado pedido, String ipDoCliente) {
        try {
            if (pedido instanceof PedidoDeSalt p) return salt(p.getUsername());
            if (pedido instanceof PedidoDeCadastro p) return cadastrar(p);
            if (pedido instanceof PedidoDeLogin p) return login(p, ipDoCliente);
            if (pedido instanceof PedidoDeListagem p) return listar(p.getToken());
            if (pedido instanceof PedidoDeCriacao p) return criar(p);
            if (pedido instanceof PedidoDeAtualizacao p) return atualizar(p);
            if (pedido instanceof PedidoDeExclusao p) return excluir(p);
            if (pedido instanceof PedidoDeExportacao p) return exportar(p.getToken());
            if (pedido instanceof PedidoDeImportacao p) return importar(p);
            return erro("DADOS_INVALIDOS");
        } catch (RuntimeException e) {
            return erro("ERRO_BANCO");
        }
    }

    private Resposta salt(String username) {
        String salt = usuarios.findByUsername(username).map(Usuario::getSalt)
                .orElseGet(() -> saltFalso.gerar(username));
        return ok(mapa("salt", salt));
    }

    private Resposta cadastrar(PedidoDeCadastro p) {
        if (vazio(p.getUsername()) || vazio(p.getSenhaMestraHash()) || vazio(p.getSalt())) {
            return erro("CAMPO_OBRIGATORIO_AUSENTE");
        }
        KeyHolder chave = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO usuario (username, senha_mestra_hash, salt) VALUES (?, ?, ?)", new String[]{"id"});
                ps.setString(1, p.getUsername());
                ps.setString(2, p.getSenhaMestraHash());
                ps.setString(3, p.getSalt());
                return ps;
            }, chave);
        } catch (DuplicateKeyException e) {
            return erro("USUARIO_JA_EXISTE");
        }
        return ok(mapa("usuarioId", chave.getKey().longValue()));
    }

    private Resposta login(PedidoDeLogin p, String ip) {
        String chaveUsuario = "usuario:" + p.getUsername();
        String chaveIp = "ip:" + ip;
        if (limiteLogin.bloqueado(chaveUsuario, LimiteLogin.MAX_FALHAS_USUARIO)
                || limiteLogin.bloqueado(chaveIp, LimiteLogin.MAX_FALHAS_IP)) {
            return erro("MUITAS_TENTATIVAS");
        }
        Optional<Usuario> usuario = usuarios.findByUsername(p.getUsername());
        if (usuario.isEmpty() || p.getHashLogin() == null || !iguais(usuario.get().getSenhaMestraHash(), p.getHashLogin())) {
            limiteLogin.registrarFalha(chaveUsuario);
            limiteLogin.registrarFalha(chaveIp);
            return erro("SENHA_INCORRETA");
        }
        limiteLogin.limpar(chaveUsuario);
        String token = jwtService.gerarToken(usuario.get().getId(), usuario.get().getUsername());
        return ok(mapa("token", token, "usuarioId", usuario.get().getId()));
    }

    private Resposta listar(String token) {
        Optional<Long> id = jwtService.extrairUsuarioId(token);
        if (id.isEmpty()) return erro("SESSAO_INVALIDA");
        List<CredencialDto> lista = new ArrayList<>();
        for (CredencialItem item : credenciais.listarCredenciais(id.get())) {
            lista.add(new CredencialDto(item.credencialId(), item.servico(), item.usuarioServico(),
                    item.dadosCifrados(), item.iv()));
        }
        return ok(mapa("credenciais", lista));
    }

    private Resposta criar(PedidoDeCriacao p) {
        Optional<Long> id = jwtService.extrairUsuarioId(p.getToken());
        if (id.isEmpty()) return erro("SESSAO_INVALIDA");
        CredentialRequest req = new CredentialRequest(p.getServico(), p.getUsuarioServico(), p.getDadosCifrados(), p.getIv());
        if (req.isInvalido()) return erro("DADOS_INVALIDOS");
        try {
            CredentialResponse r = credenciais.salvarCredencial(id.get(), req);
            return ok(mapa("credencialId", r.getCredencialId()));
        } catch (UsuarioNaoEncontradoException e) {
            return erro("USUARIO_NAO_ENCONTRADO");
        }
    }

    private Resposta atualizar(PedidoDeAtualizacao p) {
        Optional<Long> id = jwtService.extrairUsuarioId(p.getToken());
        if (id.isEmpty()) return erro("SESSAO_INVALIDA");
        CredentialRequest req = new CredentialRequest(p.getServico(), p.getUsuarioServico(), p.getDadosCifrados(), p.getIv());
        if (req.isInvalido()) return erro("DADOS_INVALIDOS");
        try {
            CredentialResponse r = credenciais.editarCredencial(p.getCredencialId(), id.get(), req);
            return ok(mapa("credencialId", r.getCredencialId(), "mensagem", "Credencial atualizada com sucesso"));
        } catch (SecurityException e) {
            return erro("ACESSO_NEGADO");
        } catch (IllegalArgumentException | UsuarioNaoEncontradoException e) {
            return erro("CREDENCIAL_NAO_ENCONTRADA");
        }
    }

    private Resposta excluir(PedidoDeExclusao p) {
        Optional<Long> id = jwtService.extrairUsuarioId(p.getToken());
        if (id.isEmpty()) return erro("SESSAO_INVALIDA");
        try {
            credenciais.excluirCredencial(p.getCredencialId(), id.get());
            return ok(mapa("mensagem", "Credencial excluída com sucesso"));
        } catch (SecurityException e) {
            return erro("ACESSO_NEGADO");
        } catch (IllegalArgumentException | UsuarioNaoEncontradoException e) {
            return erro("CREDENCIAL_NAO_ENCONTRADA");
        }
    }

    private Resposta exportar(String token) {
        Optional<Long> id = jwtService.extrairUsuarioId(token);
        if (id.isEmpty()) return erro("SESSAO_INVALIDA");
        List<CredencialDto> lista = new ArrayList<>();
        for (BackupRepository.CredencialBackup c : backup.listarCredenciais(id.get())) {
            lista.add(new CredencialDto(c.credencialId(), c.servico(), c.usuarioServico(), c.dadosCifrados(), c.iv()));
        }
        if (lista.isEmpty()) return erro("BACKUP_VAZIO");
        return ok(mapa("credenciais", lista));
    }

    private Resposta importar(PedidoDeImportacao p) {
        Optional<Long> id = jwtService.extrairUsuarioId(p.getToken());
        if (id.isEmpty()) return erro("SESSAO_INVALIDA");
        if (p.getCredenciais() == null || p.getCredenciais().isEmpty()) return erro("BACKUP_VAZIO");
        List<CredentialRequest> reqs = new ArrayList<>();
        for (CredencialDto c : p.getCredenciais()) {
            if (c == null) return erro("DADOS_INVALIDOS");
            CredentialRequest req = new CredentialRequest(c.getServico(), c.getUsuarioServico(), c.getDadosCifrados(), c.getIv());
            if (req.isInvalido()) return erro("DADOS_INVALIDOS");
            reqs.add(req);
        }
        backup.inserirCredenciais(id.get(), reqs);
        return ok(mapa("credenciaisInseridas", reqs.size()));
    }

    private static Resposta ok(Map<String, Object> dados) {
        return new Resposta(null, dados);
    }

    private static Resposta erro(String codigo) {
        return new Resposta(codigo, null);
    }

    private static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((String) pares[i], pares[i + 1]);
        }
        return m;
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
