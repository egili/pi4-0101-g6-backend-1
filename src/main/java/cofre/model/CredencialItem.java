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
}
