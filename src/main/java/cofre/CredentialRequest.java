package cofre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de requisição para salvar credencial cifrada no cofre.
 * Atende aos contratos CT2 e CT10.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialRequest(
    String servico,
    String usuarioServico,
    String dadosCifrados,
    String iv
) {
    public boolean isInvalido() {
        return vazio(servico) || vazio(usuarioServico) || vazio(dadosCifrados) || vazio(iv);
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public String getServico() { return servico; }
    public String getUsuarioServico() { return usuarioServico; }
    public String getDadosCifrados() { return dadosCifrados; }
    public String getIv() { return iv; }
}

