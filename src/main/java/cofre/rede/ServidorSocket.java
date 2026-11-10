package cofre.rede;

import cofre.protocolo.ComunicadoDeDesligamento;
import cofre.protocolo.Parceiro;
import cofre.service.NegocioServidor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Liga a aceitadora ao ciclo de vida do Spring e avisa os Clientes quando o Servidor desliga. */
@Component
public class ServidorSocket {

    private final NegocioServidor negocio;
    private final int porta;
    private final List<Parceiro> usuarios = new ArrayList<>();
    private AceitadoraDeConexao aceitadora;

    public ServidorSocket(NegocioServidor negocio, @Value("${servidor.porta:8090}") int porta) {
        this.negocio = negocio;
        this.porta = porta;
    }

    @PostConstruct
    public void iniciar() {
        try {
            aceitadora = new AceitadoraDeConexao(porta, negocio, usuarios);
            aceitadora.start();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível abrir a porta " + porta, e);
        }
    }

    public int getPorta() {
        return aceitadora.getPortaLocal();
    }

    @PreDestroy
    public void parar() throws IOException {
        synchronized (usuarios) {
            for (Parceiro usuario : usuarios) {
                try {
                    usuario.receba(new ComunicadoDeDesligamento());
                } catch (IOException ignorado) {
                    // Cliente já desconectado
                }
                usuario.adeus();
            }
            usuarios.clear();
        }
        aceitadora.fechar();
    }
}
