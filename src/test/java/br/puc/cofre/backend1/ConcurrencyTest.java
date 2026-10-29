package br.puc.cofre.backend1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcurrencyTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testarDuasChamadasParalelas() throws Exception {
        // Cria um pool de 2 threads (simulando 2 usuários)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // CountDownLatch garante que as duas threads comecem EXATAMENTE no mesmo momento
        CountDownLatch latch = new CountDownLatch(1);

        Callable<String> tarefa = () -> {
            latch.await(); // Espera o sinal de partida
            return restTemplate.getForObject("http://localhost:" + port + "/ping-lento", String.class);
        };

        // Submete as duas requisições
        Future<String> futuro1 = executor.submit(tarefa);
        Future<String> futuro2 = executor.submit(tarefa);

        long tempoInicio = System.currentTimeMillis();
        
        latch.countDown(); // Dá o sinal para as duas requisições começarem juntas

        // Pega o resultado das duas
        String resposta1 = futuro1.get();
        String resposta2 = futuro2.get();
        
        long tempoTotal = System.currentTimeMillis() - tempoInicio;

        // Validações
        assertNotNull(resposta1);
        assertNotNull(resposta2);

        // O tempo total deve ser de ~2000ms (concorrente). 
        // Se fosse bloqueante, seria ~4000ms.
        assertTrue(tempoTotal < 3500, "O servidor demorou muito, parece que travou!");
        
        executor.shutdown();
    }
}