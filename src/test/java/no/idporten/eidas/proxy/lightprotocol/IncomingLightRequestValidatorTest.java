package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.lightprotocol.messages.RequestedAttribute;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncomingLightRequestValidatorTest {

    @Test
    void validateRequest_WhenRequestIsNull_ShouldThrowException() {
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(null));
    }

    @Test
    void validateRequest_WhenCitizenCountryCodeIsInvalid_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setCitizenCountryCode("US"); // Invalid country code
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenIdIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setId(""); // Empty ID
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenIssuerIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setIssuer(""); // Empty issuer
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenProviderNameIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setProviderName(""); // Empty provider name
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenSpTypeIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setSpType(""); // Empty service provider type
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenSpCountryCodeIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setSpCountryCode(""); // Empty SP country code
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenRelayStateIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setRelayState(""); // Empty relay state
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenRequestedAttributesIsEmpty_ShouldThrowException() {
        LightRequest request = createValidLightRequest();
        request.setRequestedAttributes(Collections.emptyList()); // Empty list of requested attributes
        assertThrows(SpecificProxyException.class, () -> IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenAllFieldsAreValid_ShouldReturnTrue() {
        LightRequest request = createValidLightRequest();
        assertDoesNotThrow(() -> IncomingLightRequestValidator.validateRequest(request));
    }

    private LightRequest createValidLightRequest() {
        return LightRequest.builder()
                .citizenCountryCode("NO")
                .id("12345")
                .spCountryCode("CA")
                .levelOfAssurance(new LevelOfAssurance("notified", "http://eidas.europa.eu/LoA/low"))
                .providerName("DEMO-SP-CA")
                .relayState("abcd-efgh-ijkl")
                .issuer("http://example.com")
                .spType("public")
                .requestedAttributes(List.of(new RequestedAttribute(null, "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName")))
                .build();
    }
}
