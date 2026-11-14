package cofre;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cofre.protocolo.PedidoDeCadastro;
import cofre.protocolo.PedidoDeLogin;
import cofre.protocolo.Resposta;
import cofre.service.NegocioServidor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "servidor.porta=0",
        "servidor.comandos=false",
        "jwt.secret=chave-de-teste-do-cofre-com-mais-de-32-caracteres",
        "jwt.expiration=86400"
})
public class LogSegurancaTest {

    @Autowired
    private NegocioServidor negocio;

    private final Logger logCofre = (Logger) LoggerFactory.getLogger("cofre");
    private ListAppender<ILoggingEvent> captura;

    @BeforeEach
    public void capturaLog() {
        captura = new ListAppender<>();
        captura.start();
        logCofre.addAppender(captura);
    }

    @AfterEach
    public void paraCaptura() {
        logCofre.detachAppender(captura);
    }

    @Test
    public void logRegistraUsuarioMasNuncaSegredos() {
        String usuario = "logteste" + System.nanoTime();
        String hashCorreto = "HASH-SECRETO-CORRETO-9f3a";
        String hashErrado = "HASH-SECRETO-ERRADO-7c1b";

        negocio.processar(new PedidoDeCadastro(usuario, hashCorreto, "salt-de-teste"), "10.1.1.1");
        negocio.processar(new PedidoDeLogin(usuario, hashErrado), "10.1.1.1");
        Resposta ok = negocio.processar(new PedidoDeLogin(usuario, hashCorreto), "10.1.1.1");
        String token = (String) ok.getDados().get("token");

        String registro = captura.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertTrue(registro.contains(usuario), "o nome do usuário deve aparecer no log");
        assertFalse(registro.contains(hashCorreto), "o hash da senha mestra não pode aparecer no log");
        assertFalse(registro.contains(hashErrado), "o hash digitado não pode aparecer no log");
        assertFalse(registro.contains(token), "o token não pode aparecer no log");
    }
}
