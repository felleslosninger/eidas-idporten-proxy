package no.idporten.eidas.proxy.lightprotocol;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.exceptions.ErrorCodes;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import org.springframework.util.CollectionUtils;

/**
 * Add some validation to the incoming light request
 */
@Slf4j
public class IncomingLightRequestValidator {
    private IncomingLightRequestValidator() {
    }

    public static boolean validateRequest(LightRequest lightRequest) throws SpecificProxyException {
        if (lightRequest == null) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is null.", null);
        }
        if (!"NO".equals(lightRequest.getCitizenCountryCode())) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is not from Norwegian citizen.", lightRequest);
        }
        if (lightRequest.getId().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing an id.", lightRequest);
        }
        if (lightRequest.getIssuer().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing an issuer.", lightRequest);
        }

        if (lightRequest.getProviderName() == null || lightRequest.getProviderName().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing a provider name.", lightRequest);
        }
        if (lightRequest.getSpType() == null || lightRequest.getSpType().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing a service provider type.", lightRequest);
        }
        if (lightRequest.getSpCountryCode() == null || lightRequest.getSpCountryCode().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing an SP country code.", lightRequest);
        }
        if (lightRequest.getRelayState() == null || lightRequest.getRelayState().isEmpty()) {
            log.warn("Incoming Light Request is missing a relay state: {}", lightRequest);
        }
        if (CollectionUtils.isEmpty(lightRequest.getRequestedAttributesList())) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing requested attributes.", lightRequest);
        }

        //we made it
        return true;
    }
}
