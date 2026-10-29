package br.puc.cofre.backend1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "servico", "Backend 1 - Servidor");
    }

    // Rota nova para testar concorrência
    @GetMapping("/ping-lento")
    public Map<String, String> pingLento() throws InterruptedException {
        Thread.sleep(2000); // Simula um processamento de 2 segundos
        return Map.of("status", "ok", "mensagem", "Processado sem travar os outros");
    }
}