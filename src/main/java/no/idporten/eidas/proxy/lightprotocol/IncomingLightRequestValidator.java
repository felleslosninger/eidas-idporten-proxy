package no.idporten.eidas.proxy.lightprotocol;

import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import org.springframework.util.CollectionUtils;

/**
 * Add some validation to the incoming light request
 */
@Slf4j
public class IncomingLightRequestValidator {

    public static boolean validateRequest(LightRequest lightRequest) throws SpecificCommunicationException {
        if (lightRequest == null) {
            throw new SpecificCommunicationException("Incoming Light Request is null.");
        }
        if (!"NO".equals(lightRequest.getCitizenCountryCode())) {
            throw new SpecificCommunicationException("Incoming Light Request is not from Norwegian citizen.");
        }
        if (lightRequest.getId() == null || lightRequest.getId().isEmpty()) {
            throw new SpecificCommunicationException("Incoming Light Request is missing an id.");
        }
        if (lightRequest.getIssuer() == null || lightRequest.getIssuer().isEmpty()) {
            throw new SpecificCommunicationException("Incoming Light Request is missing an issuer.");
        }

        if (lightRequest.getProviderName() == null || lightRequest.getProviderName().isEmpty()) {
            throw new SpecificCommunicationException("Incoming Light Request is missing a provider name.");
        }
        if (lightRequest.getSpType() == null || lightRequest.getSpType().isEmpty()) {
            throw new SpecificCommunicationException("Incoming Light Request is missing a service provider type.");
        }
        if (lightRequest.getSpCountryCode() == null || lightRequest.getSpCountryCode().isEmpty()) {
            throw new SpecificCommunicationException("Incoming Light Request is missing an SP country code.");
        }
        if (lightRequest.getRelayState() == null || lightRequest.getRelayState().isEmpty()) {
            throw new SpecificCommunicationException("Incoming Light Request is missing a relay state.");
        }
        if (CollectionUtils.isEmpty(lightRequest.getRequestedAttributesList())) {
            throw new SpecificCommunicationException("Incoming Light Request is missing requested attributes.");
        }

        //we made it
        return true;
    }
}
