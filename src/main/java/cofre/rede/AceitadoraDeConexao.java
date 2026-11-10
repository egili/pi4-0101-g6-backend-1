package cofre.rede;

import cofre.protocolo.Parceiro;
import cofre.service.NegocioServidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/** Aceita conexões de Clientes. Há uma só, e ela roda numa thread própria. */
public class AceitadoraDeConexao extends Thread {

    private final ServerSocket pedido;
    private final NegocioServidor negocio;
    private final List<Parceiro> usuarios;

    public AceitadoraDeConexao(int porta, NegocioServidor negocio, List<Parceiro> usuarios) throws IOException {
        this.pedido = new ServerSocket(porta);
        this.negocio = negocio;
        this.usuarios = usuarios;
        setName("aceitadora-de-conexao");
        setDaemon(true);
    }

    public int getPortaLocal() {
        return pedido.getLocalPort();
    }

    @Override
    public void run() {
        while (!pedido.isClosed()) {
            try {
                Socket conexao = pedido.accept();
                new SupervisoraDeConexao(conexao, negocio, usuarios).start();
            } catch (IOException e) {
                if (pedido.isClosed()) {
                    break;
                }
            }
        }
    }

    public void fechar() throws IOException {
        pedido.close();
    }
}
