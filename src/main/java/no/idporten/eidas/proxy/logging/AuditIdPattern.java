package no.idporten.eidas.proxy.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import no.idporten.logging.audit.AuditIdentifier;

@Getter
@AllArgsConstructor
public enum AuditIdPattern {
    EIDAS_LIGHT_REQUEST("%s-LIGHT-REQUEST"),
    EIDAS_LIGHT_RESPONSE("%s-LIGHT-RESPONSE");

    private String pattern;

    public AuditIdentifier auditIdentifier() {
        return () -> String.format(getPattern(), "EIDAS-IDPORTEN-PROXY");
    }
}