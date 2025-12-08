package no.idporten.eidas.proxy.logging;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.PushedAuthorizationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.logging.audit.AuditDataProvider;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private static final String RELATED_TRACE_ID = "related_trace_id";
    private static final String DIGDIR_IDP = "digdir_idp";
    private static final String LIGHT_REQUEST = "light_request";
    private static final String LIGHT_RESPONSE = "light_response";
    private static final String IDP_PAR_REQUEST = "idp_par_request";
    private final AuditLogger auditLogger;


    public void auditLightRequest(LightRequest lightRequest, String idp) {
        log(AuditIdPattern.EIDAS_LIGHT_REQUEST, LIGHT_REQUEST, lightRequest, idp);
    }

    public void auditLightResponse(LightResponse lightResponse, String relatedTraceId, String idp) {
        log(AuditIdPattern.EIDAS_LIGHT_RESPONSE, LIGHT_RESPONSE, lightResponse, relatedTraceId, idp);
    }

    public void auditIDPParRequest(PushedAuthorizationRequest parRequest, String relatedTraceId, String idp) {
        try {
            AuthorizationRequest r = parRequest.getAuthorizationRequest();
            auditLogger.log(AuditEntry.builder()
                    .auditId(AuditIdPattern.EIDAS_IDP_PAR_REQUEST.auditIdentifier())
                    .logNullAttributes(false)
                    .attribute(RELATED_TRACE_ID, relatedTraceId)
                    .attribute(DIGDIR_IDP, idp)
                    .attribute(IDP_PAR_REQUEST, "token_endpoint=%s, client_id=%s, redirect_uri=%s, custom_parameters=%s"
                            .formatted(r.getEndpointURI(), r.getClientID(), r.getRedirectionURI().toString(), getCustomParametersAsString(r)))
                    .build());
        } catch (Exception e) {
            log.error("Error while auditing par request", e);
        }
    }

    private static String getCustomParametersAsString(AuthorizationRequest r) {
        Map<String, List<String>> params = r.getCustomParameters();
        if (CollectionUtils.isEmpty(params)) {
            return "";
        }

        return params.entrySet().stream()
                .flatMap(entry -> {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    if (values == null || values.isEmpty()) {
                        return Stream.of(key + ", null");
                    }
                    return values.stream().map(v -> key + ", " + v);
                })
                .collect(Collectors.joining(", "));
    }

    private void log(AuditIdPattern auditIdPattern, String auditDataAttribute, AuditDataProvider auditDataProvider, String idp) {
        log(auditIdPattern, auditDataAttribute, auditDataProvider, null, idp);
    }

    private void log(AuditIdPattern auditIdPattern, String auditDataAttribute, AuditDataProvider auditDataProvider, String relatedTraceId, String idp) {
        try {
            auditLogger.log(AuditEntry.builder()
                    .auditId(auditIdPattern.auditIdentifier())
                    .logNullAttributes(false)
                    .attribute(RELATED_TRACE_ID, relatedTraceId)
                    .attribute(DIGDIR_IDP, idp)
                    .attribute(auditDataAttribute, auditDataProvider.getAuditData().getAttributes())
                    .build());
        } catch (Exception e) {
            log.error("Error while auditing light message", e);
        }
    }

}
