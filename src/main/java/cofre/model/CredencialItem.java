package cofre.model;

/**
 * Item da listagem de credenciais (CT4).
 * Só expõe o blob cifrado e o IV persistidos na tabela credencial (CT10).
 */
public record CredencialItem(
        Long credencialId,
        String servico,
        String usuarioServico,
        String dadosCifrados,
        String iv
) {
    public Long getCredencialId() { return credencialId; }
    public String getServico() { return servico; }
    public String getUsuarioServico() { return usuarioServico; }
    public String getDadosCifrados() { return dadosCifrados; }
    public String getIv() { return iv; }
}
