package cofre.rede;

import cofre.protocolo.ComunicadoDeDesligamento;
import cofre.protocolo.Parceiro;
import cofre.service.NegocioServidor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Liga a aceitadora ao ciclo de vida do Spring, avisa os Clientes ao desligar e lê o comando "desativar". */
@Component
public class ServidorSocket {

    private final NegocioServidor negocio;
    private final ApplicationContext contexto;
    private final int porta;
    private final boolean lerComandos;
    private final List<Parceiro> usuarios = new ArrayList<>();
    private AceitadoraDeConexao aceitadora;

    public ServidorSocket(NegocioServidor negocio, ApplicationContext contexto,
                          @Value("${servidor.porta:8090}") int porta,
                          @Value("${servidor.comandos:true}") boolean lerComandos) {
        this.negocio = negocio;
        this.contexto = contexto;
        this.porta = porta;
        this.lerComandos = lerComandos;
    }

    @PostConstruct
    public void iniciar() {
        try {
            aceitadora = new AceitadoraDeConexao(porta, negocio, usuarios);
            aceitadora.start();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível abrir a porta " + porta, e);
        }
        if (lerComandos) {
            Thread comandos = new Thread(this::lerComandos, "comandos-do-servidor");
            comandos.setDaemon(true);
            comandos.start();
        }
    }

    public int getPorta() {
        return aceitadora.getPortaLocal();
    }

    /** Comando "desativar" no terminal do Servidor: avisa todos os Clientes e encerra o programa. */
    private void lerComandos() {
        System.out.println("O servidor esta ativo! Para desativa-lo, use o comando \"desativar\"");
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.trim().equalsIgnoreCase("desativar")) {
                    System.out.println("O servidor foi desativado!");
                    System.exit(SpringApplication.exit(contexto, () -> 0));
                }
                System.err.println("Comando invalido!");
            }
        } catch (IOException e) {
            // terminal fechado: o Servidor segue sem comandos
        }
    }

    /** Avisa todos os Clientes conectados e fecha as conexões. */
    public void avisarDesligamento() {
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
    }

    @PreDestroy
    public void parar() throws IOException {
        avisarDesligamento();
        aceitadora.fechar();
    }
}
