package no.idporten.eidas.proxy.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.logging.audit.AuditDataProvider;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private static final String RELATED_TRACE_ID = "related_trace_id";
    private static final String LIGHT_REQUEST = "light_request";
    private static final String LIGHT_RESPONSE = "light_response";
    private final AuditLogger auditLogger;

    private void log(AuditIdPattern auditIdPattern, String auditDataAttribute, AuditDataProvider auditDataProvider) {
        log(auditIdPattern, auditDataAttribute, auditDataProvider, null);
    }

    private void log(AuditIdPattern auditIdPattern, String auditDataAttribute, AuditDataProvider auditDataProvider, String relatedTraceId) {
        try {
            auditLogger.log(AuditEntry.builder()
                    .auditId(auditIdPattern.auditIdentifier())
                    .logNullAttributes(false)
                    .attribute(RELATED_TRACE_ID, relatedTraceId)
                    .attribute(auditDataAttribute, auditDataProvider.getAuditData().getAttributes())
                    .build());
        } catch (Exception e) {
            log.error("Error while auditing light message", e);
        }
    }

    public void auditLightRequest(LightRequest lightRequest) {
        log(AuditIdPattern.EIDAS_LIGHT_REQUEST, LIGHT_REQUEST, lightRequest);
    }

    public void auditLightResponse(LightResponse lightResponse, String relatedTraceId) {
        log(AuditIdPattern.EIDAS_LIGHT_RESPONSE, LIGHT_RESPONSE, lightResponse, relatedTraceId);
    }

}
