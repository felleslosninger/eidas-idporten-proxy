package no.idporten.eidas.proxy.exceptions;

import eu.eidas.auth.commons.EIDASStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.logging.AuditService;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handling for eidas-idporten-proxy.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class WebExceptionControllerAdvice {

    private final SpecificProxyService specificProxyService;
    private final SpecificCommunicationService specificCommunicationService;
    private final AuditService auditService;

    @ExceptionHandler(SpecificProxyException.class)
    public String handleOAuth2Exception(SpecificProxyException ex) {

        log.error("SpecificProxyException occurred for request: {} {}", ex.getLightRequest(), ex.getMessage());

        LightResponse lightResponse = specificProxyService.getErrorLightResponse(EIDASStatusCode.REQUESTER_URI, ex);
        auditService.auditLightResponse(lightResponse);
        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);
        return "redirect:%s?token=%s".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64);
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex) {
        log.error("Exception occurred :{}", ex.getMessage());
        LightResponse lightResponse = specificProxyService.getErrorLightResponse(EIDASStatusCode.RESPONDER_URI, ex);
        auditService.auditLightResponse(lightResponse);
        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);
        return "redirect:%s?token=%s".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64);
    }

}
