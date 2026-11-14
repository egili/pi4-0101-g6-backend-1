package cofre.rede;

import cofre.protocolo.Comunicado;
import cofre.protocolo.Parceiro;
import cofre.protocolo.PedidoParaSair;
import cofre.protocolo.Resposta;
import cofre.service.NegocioServidor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Atende um Cliente. Há uma thread destas para cada conexão aceita. */
public class SupervisoraDeConexao extends Thread {

    private static final Logger log = LoggerFactory.getLogger(SupervisoraDeConexao.class);

    private final Socket conexao;
    private final NegocioServidor negocio;
    private final List<Parceiro> usuarios;
    private Parceiro usuario;
    private String ip;

    public SupervisoraDeConexao(Socket conexao, NegocioServidor negocio, List<Parceiro> usuarios) {
        this.conexao = conexao;
        this.negocio = negocio;
        this.usuarios = usuarios;
        setName("supervisora-" + conexao.getPort());
        setDaemon(true);
    }

    @Override
    public void run() {
        ip = conexao.getInetAddress().getHostAddress();
        log.info("Conexão aberta: {}", ip);
        try {
            ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
            ObjectInputStream receptor = Parceiro.novoReceptor(conexao.getInputStream());
            usuario = new Parceiro(conexao, receptor, transmissor);
            synchronized (usuarios) {
                usuarios.add(usuario);
            }
            for (;;) {
                Comunicado pedido = usuario.envie();
                if (pedido == null || pedido instanceof PedidoParaSair) {
                    break;
                }
                Resposta resposta = negocio.processar(pedido, ip);
                usuario.receba(resposta);
            }
        } catch (IOException | ClassNotFoundException e) {
            log.warn("Conexão de {} encerrada com erro: {}", ip, e.getClass().getSimpleName());
            // conexão caiu ou mensagem inválida: encerra só esta conexão
        } finally {
            synchronized (usuarios) {
                usuarios.remove(usuario);
            }
            log.info("Conexão fechada: {}", ip);
            if (usuario != null) {
                usuario.adeus();
            }
        }
    }
}
