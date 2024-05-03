package no.idporten.eidas.proxy.lightprotocol;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomingLightRequestValidatorTest {

    @Test
    void validateRequest_WhenRequestIsNull_ShouldReturnFalse() {
        assertFalse(IncomingLightRequestValidator.validateRequest(null));
    }

    @Test
    void validateRequest_WhenCitizenCountryCodeIsInvalid_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setCitizenCountryCode("US"); // Invalid country code
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenIdIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setId(""); // Empty ID
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenIssuerIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setIssuer(""); // Empty issuer
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenLevelOfAssuranceIsLow_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setLevelOfAssurance(new LevelOfAssurance("notified", "http://eidas.europa.eu/LoA/high")); // Not low
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenProviderNameIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setProviderName(""); // Empty provider name
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenSpTypeIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setSpType(""); // Empty service provider type
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenSpCountryCodeIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setSpCountryCode(""); // Empty SP country code
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenRelayStateIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setRelayState(""); // Empty relay state
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenRequestedAttributesIsEmpty_ShouldReturnFalse() {
        LightRequest request = createValidLightRequest();
        request.setRequestedAttributes(Collections.emptyList()); // Empty list of requested attributes
        assertFalse(IncomingLightRequestValidator.validateRequest(request));
    }

    @Test
    void validateRequest_WhenAllFieldsAreValid_ShouldReturnTrue() {
        LightRequest request = createValidLightRequest();
        assertTrue(IncomingLightRequestValidator.validateRequest(request));
    }

    private LightRequest createValidLightRequest() {
        return new LightRequest(
                "NO",
                "12345",
                "http://example.com",
                new LevelOfAssurance("notified", "http://eidas.europa.eu/LoA/low"),
                "Provider",
                "public",
                "NO",
                "abcd-efgh-ijkl",
                List.of(new Attribute("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName"))
        );
    }
}
