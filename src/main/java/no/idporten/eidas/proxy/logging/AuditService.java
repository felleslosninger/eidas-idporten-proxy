package no.idporten.eidas.proxy.logging;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogger auditLogger;

    public void auditLightRequest(LightRequest lightRequest) {
        auditLogger.log(lightRequest.getAuditEntry());
    }

    public void auditLightResponse(LightResponse lightResponse) {
        auditLogger.log(lightResponse.getAuditEntry());
    }

}
