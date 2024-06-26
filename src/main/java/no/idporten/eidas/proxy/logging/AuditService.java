package no.idporten.eidas.proxy.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditEntryProvider;
import no.idporten.logging.audit.AuditIdentifier;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogger auditLogger;

    @Getter
    @AllArgsConstructor
    enum AuditIdPattern {
        EIDAS_LIGHT_REQUEST("%s-LIGHT-REQUEST"),
        EIDAS_LIGHT_RESPONSE("%s-LIGHT-RESPONSE");

        private String pattern;

        AuditIdentifier auditIdentifier() {
            return () -> String.format(getPattern(), "EIDAS_IDPORTEN_PROXY");
        }
    }

    private void log(AuditIdPattern auditIdPattern, String AuditEntryAttribute, AuditEntryProvider AuditEntryProvider) {
        auditLogger.log(AuditEntry.builder()
                .auditId(auditIdPattern.auditIdentifier())
                .logNullAttributes(false)
                .attribute(AuditEntryAttribute, AuditEntryProvider.getAuditEntry().getAttributes())
                .build());
    }

    public void auditLightRequest(LightRequest lightRequest) {
        log(AuditIdPattern.EIDAS_LIGHT_REQUEST, "light_request", lightRequest);
    }

    public void auditLightResponse(LightResponse lightResponse) {
        log(AuditIdPattern.EIDAS_LIGHT_RESPONSE, "light_response", lightResponse);
    }

}
