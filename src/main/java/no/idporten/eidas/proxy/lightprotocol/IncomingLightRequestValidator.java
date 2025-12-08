package no.idporten.eidas.proxy.lightprotocol;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.exceptions.ErrorCodes;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.service.IDPSelector;
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
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is null.", null, null);
        }
        String idp = IDPSelector.chooseIdp(lightRequest.getRequestedAttributesAsStringSet());
        if (!"NO".equals(lightRequest.getCitizenCountryCode())) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is not from Norwegian citizen.", lightRequest, idp);
        }
        if (lightRequest.getId().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing an id.", lightRequest, idp);
        }
        if (lightRequest.getIssuer().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing an issuer.", lightRequest, idp);
        }

        if (lightRequest.getProviderName() == null || lightRequest.getProviderName().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing a provider name.", lightRequest, idp);
        }
        if (lightRequest.getSpType() == null || lightRequest.getSpType().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing a service provider type.", lightRequest, idp);
        }
        if (lightRequest.getSpCountryCode() == null || lightRequest.getSpCountryCode().isEmpty()) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing an SP country code.", lightRequest, idp);
        }
        if (lightRequest.getRelayState() == null || lightRequest.getRelayState().isEmpty()) {
            log.warn("Incoming Light Request is missing a relay state: {}", lightRequest);
        }
        if (CollectionUtils.isEmpty(lightRequest.getRequestedAttributesList())) {
            throw new SpecificProxyException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Request is missing requested attributes.", lightRequest, idp);
        }

        //we made it
        return true;
    }
}
