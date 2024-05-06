package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import org.springframework.util.CollectionUtils;

/**
 * Add some validation to the incoming light request
 */
public class IncomingLightRequestValidator {

    public static boolean validateRequest(LightRequest lightRequest) {
        if (lightRequest == null) {
            return false;
        }
        if (!"NO".equals(lightRequest.getCitizenCountryCode())) {
            return false;
        }
        if (lightRequest.getId() == null || lightRequest.getId().isEmpty()) {
            return false;
        }
        if (lightRequest.getIssuer() == null || lightRequest.getIssuer().isEmpty()) {
            return false;
        }

        if (lightRequest.getProviderName() == null || lightRequest.getProviderName().isEmpty()) {
            return false;
        }
        if (lightRequest.getSpType() == null || lightRequest.getSpType().isEmpty()) {
            return false;
        }
        if (lightRequest.getSpCountryCode() == null || lightRequest.getSpCountryCode().isEmpty()) {
            return false;
        }
        if (lightRequest.getRelayState() == null || lightRequest.getRelayState().isEmpty()) {
            return false;
        }
        return !CollectionUtils.isEmpty(lightRequest.getRequestedAttributesList());
    }
}
