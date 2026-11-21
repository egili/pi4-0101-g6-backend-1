package cofre.protocolo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParceiroFiltroTest {

    private static byte[] serializar(Object objeto) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream saida = new ObjectOutputStream(bytes)) {
            saida.writeObject(objeto);
        }
        return bytes.toByteArray();
    }

    @Test
    public void classeForaDaAllowlistEhRecusada() throws IOException {
        byte[] mensagem = serializar(new LinkedList<String>());

        try (ObjectInputStream receptor = Parceiro.novoReceptor(new ByteArrayInputStream(mensagem))) {
            assertThrows(InvalidClassException.class, receptor::readObject);
        }
    }

    @Test
    public void mensagemDoProtocoloEColecoesPermitidasPassam() throws Exception {
        Map<String, Object> dados = new HashMap<>();
        dados.put("usuarioId", 7L);
        dados.put("credenciaisInseridas", 2);
        dados.put("mensagem", "ok");
        byte[] resposta = serializar(new Resposta(null, dados));
        byte[] pedido = serializar(new PedidoParaSair());

        try (ObjectInputStream receptor = Parceiro.novoReceptor(new ByteArrayInputStream(resposta))) {
            Resposta lida = (Resposta) receptor.readObject();
            assertEquals(7L, lida.getDados().get("usuarioId"));
        }
        try (ObjectInputStream receptor = Parceiro.novoReceptor(new ByteArrayInputStream(pedido))) {
            assertTrue(receptor.readObject() instanceof PedidoParaSair);
        }
    }
}
