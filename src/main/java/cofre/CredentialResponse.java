package cofre;

/**
 * DTO de resposta para salvamento de credencial no cofre.
 * Atende ao contrato CT2.
 */
public record CredentialResponse(
    Long credencialId,
    String mensagem
) {
    public Long getCredencialId() { return credencialId; }
    public String getMensagem() { return mensagem; }
}

