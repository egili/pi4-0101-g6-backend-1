package cofre.rede;

import cofre.protocolo.Parceiro;
import cofre.service.NegocioServidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Aceita conexões de Clientes. Há uma só, e ela roda numa thread própria. */
public class AceitadoraDeConexao extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AceitadoraDeConexao.class);

    private final ServerSocket pedido;
    private final NegocioServidor negocio;
    private final List<Parceiro> usuarios;

    public AceitadoraDeConexao(int porta, NegocioServidor negocio, List<Parceiro> usuarios) throws IOException {
        this.pedido = new ServerSocket(porta);
        this.negocio = negocio;
        this.usuarios = usuarios;
        setName("aceitadora-de-conexao");
        setDaemon(true);
        log.info("Aguardando conexões na porta {}", pedido.getLocalPort());
    }

    public int getPortaLocal() {
        return pedido.getLocalPort();
    }

    @Override
    public void run() {
        while (!pedido.isClosed()) {
            try {
                Socket conexao = pedido.accept();
                log.debug("Conexão recebida de {}", conexao.getRemoteSocketAddress());
                new SupervisoraDeConexao(conexao, negocio, usuarios).start();
            } catch (IOException e) {
                if (pedido.isClosed()) {
                    break;
                }
                log.warn("Falha ao aceitar conexão: {}", e.getMessage());
            }
        }
    }

    public void fechar() throws IOException {
        pedido.close();
    }
}
