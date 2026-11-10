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

/** Atende um Cliente. Há uma thread destas para cada conexão aceita. */
public class SupervisoraDeConexao extends Thread {

    private final Socket conexao;
    private final NegocioServidor negocio;
    private final List<Parceiro> usuarios;
    private Parceiro usuario;

    public SupervisoraDeConexao(Socket conexao, NegocioServidor negocio, List<Parceiro> usuarios) {
        this.conexao = conexao;
        this.negocio = negocio;
        this.usuarios = usuarios;
        setName("supervisora-" + conexao.getPort());
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
            ObjectInputStream receptor = Parceiro.novoReceptor(conexao.getInputStream());
            usuario = new Parceiro(conexao, receptor, transmissor);
            synchronized (usuarios) {
                usuarios.add(usuario);
            }
            String ip = conexao.getInetAddress().getHostAddress();
            for (;;) {
                Comunicado pedido = usuario.envie();
                if (pedido == null || pedido instanceof PedidoParaSair) {
                    break;
                }
                Resposta resposta = negocio.processar(pedido, ip);
                usuario.receba(resposta);
            }
        } catch (IOException | ClassNotFoundException e) {
            // conexão caiu ou mensagem inválida: encerra só esta conexão
        } finally {
            synchronized (usuarios) {
                usuarios.remove(usuario);
            }
            if (usuario != null) {
                usuario.adeus();
            }
        }
    }
}
