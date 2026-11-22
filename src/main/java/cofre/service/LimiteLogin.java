package cofre.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia novas tentativas de login depois de falhas seguidas, por usuário e por IP.
 * shortcut: contadores em memória desta instância; com mais de uma instância do Servidor, usar um store compartilhado (ex.: Redis).
 */
@Component
public class LimiteLogin {

    public static final int MAX_FALHAS_USUARIO = 5;
    public static final int MAX_FALHAS_IP = 20;
    static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMITE_DE_CHAVES = 10_000;

    private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();

    public boolean bloqueado(String chave, int maximo) {
        Falhas f = falhas.get(chave);
        return f != null && f.dentroDaJanela(Instant.now()) && f.total() >= maximo;
    }

    public void registrarFalha(String chave) {
        Instant agora = Instant.now();
        if (falhas.size() > LIMITE_DE_CHAVES) {
            falhas.values().removeIf(f -> !f.dentroDaJanela(agora));
        }
        falhas.compute(chave, (k, f) -> f == null || !f.dentroDaJanela(agora)
                ? new Falhas(1, agora)
                : new Falhas(f.total() + 1, f.inicio()));
    }

    public void limpar(String chave) {
        falhas.remove(chave);
    }

    private record Falhas(int total, Instant inicio) {
        boolean dentroDaJanela(Instant agora) {
            return agora.isBefore(inicio.plus(JANELA));
        }
    }
}
