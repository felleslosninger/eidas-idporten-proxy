package no.idporten.eidas.proxy.service;

import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.impl.LightRequest;
import no.idporten.eidas.proxy.config.EuProxyProperties;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;

import static no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService.E_JUSTICE_NATURAL_PERSON_ROLE_CLAIM;
import static no.idporten.eidas.proxy.service.EidasAttributeNames.E_JUSTICE_NATURAL_PERSON_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SpecificProxyServiceTest {

    @Mock
    private SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    @Mock
    private OIDCRequestCache oidcRequestCache;
    @Mock
    private OIDCIntegrationService oidcIntegrationService;
    @Mock
    private EuProxyProperties euProxyProperties;

    @Mock
    private LevelOfAssuranceHelper levelOfAssuranceHelper;

    @Mock
    private ILightRequest mockLightRequest;
    @InjectMocks
    SpecificProxyService specificProxyService;


    @BeforeEach
    void setup() {
        when(oidcIntegrationService.getIssuer(any())).thenReturn("Issuer");
        when(mockLightRequest.getRelayState()).thenReturn("relay123");
    }

    @Test
    @DisplayName("when buildLightResponse then return LightResponse without validation errors")
    void buildLightResponse() throws SpecificProxyException {
        UserInfo userInfo = new UserInfo(new Subject("123456789"));

        // Populate standard claims
        userInfo.setGivenName("John");
        userInfo.setFamilyName("Smith");
        userInfo.setClaim("pid", "123456789");
        userInfo.setClaim("birth_date", "1990-01-01");

        ILightRequest lightRequest = LightRequest.builder()
                .id("myid")
                .issuer("http://euproxy")
                .levelOfAssurance("http://eidas.europa.eu/LoA/low")
                .relayState("myrelaystate")
                .citizenCountryCode("NO")
                .build();

        OIDCIntegrationService mockOidcIntegrationService = mock(OIDCIntegrationService.class);
        when(mockOidcIntegrationService.getIssuer(IDPSelector.IDPORTEN)).thenReturn("http://myjunit");
        LightResponse lightResponse = specificProxyService.getLightResponse(IDPSelector.IDPORTEN, userInfo, lightRequest, LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_LOW));
        assertNotNull(lightResponse);
    }

    @Test
    @DisplayName("when a specific exception return LightResponse with detailed error for SpecificProxyException")
    void testGetErrorLightResponseWithSpecificProxyException() {

        SpecificProxyException spex = new SpecificProxyException("Error code", "Error message", mockLightRequest, IDPSelector.IDPORTEN);

        LightResponse result = specificProxyService.getErrorLightResponse(IDPSelector.IDPORTEN, EIDASStatusCode.REQUESTER_URI, spex);

        assertAll("Verifying all properties of the LightResponse for SpecificProxyException",
                () -> assertNotNull(result.getId(), "ID must not be null"),
                () -> assertEquals("NO", result.getCitizenCountryCode(), "CitizenCountryCode must be 'NO'"),
                () -> assertEquals("Issuer", result.getIssuer(), "Issuer must match the expected value"),
                () -> assertEquals("relay123", result.getRelayState(), "RelayState must be 'relay123'"),
                () -> assertEquals("Error message", result.getStatus().getStatusMessage(), "Status message must match the error message from the exception"),
                () -> assertEquals(EIDASStatusCode.REQUESTER_URI.getValue(), result.getStatus().getStatusCode(), "Status code must match the expected value")
        );
    }

    @Test
    @DisplayName("must return LightResponse with generic error for non-specific exceptions")
    void testGetErrorLightResponseWithGenericException() {

        Exception ex = new Exception("Generic error");


        LightResponse result = specificProxyService.getErrorLightResponse(IDPSelector.IDPORTEN, EIDASStatusCode.REQUESTER_URI, ex);

        assertAll("must properly handle generic exceptions and form correct LightResponse",
                () -> assertNotNull(result.getId(), "ID must not be null"),
                () -> assertEquals("NO", result.getCitizenCountryCode(), "CitizenCountryCode must be 'NO'"),
                () -> assertEquals("Issuer", result.getIssuer(), "Issuer must match the expected value"),
                () -> assertNull(result.getRelayState(), "RelayState will be null for generic exceptions"),
                () -> assertEquals("An internal error occurred", result.getStatus().getStatusMessage(), "Status message must reflect an internal error"),
                () -> assertEquals(EIDASStatusCode.REQUESTER_URI.getValue(), result.getStatus().getStatusCode(), "Status code must match the expected value")
        );
    }

    @Test
    @DisplayName("should include E_JUSTICE_NATURAL_PERSON_ROLE attribute when role VIP1 claim is present")
    void includesEJusticeAttributeWhenVip1ClaimPresent() {
        UserInfo userInfo = new UserInfo(new Subject("sub"));
        userInfo.setClaim(E_JUSTICE_NATURAL_PERSON_ROLE_CLAIM, "VIP1");

        ILightRequest lightRequest = LightRequest.builder()
                .id("id1")
                .relayState("relay")
                .issuer("issuer")
                .levelOfAssurance("http://eidas.europa.eu/LoA/low")
                .citizenCountryCode("NO")
                .build();

        LightResponse lr = specificProxyService.getLightResponse(IDPSelector.ANSATTPORTEN, userInfo, lightRequest, LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_LOW));
        Set<String> attributeNames = lr.getRequestedAttributesAsStringSet();

        assertTrue(attributeNames.contains(E_JUSTICE_NATURAL_PERSON_ROLE));
    }

    @Test
    @DisplayName("should include E_JUSTICE_NATURAL_PERSON_ROLE attribute when role VIP2 claim is present")
    void includesEJusticeAttributeWhenVip2ClaimPresent() {
        UserInfo userInfo = new UserInfo(new Subject("sub"));
        userInfo.setClaim(E_JUSTICE_NATURAL_PERSON_ROLE_CLAIM, "VIP2");

        ILightRequest lightRequest = LightRequest.builder()
                .id("id1")
                .relayState("relay")
                .issuer("issuer")
                .levelOfAssurance("http://eidas.europa.eu/LoA/low")
                .citizenCountryCode("NO")
                .build();

        LightResponse lr = specificProxyService.getLightResponse(IDPSelector.ANSATTPORTEN, userInfo, lightRequest, LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_LOW));
        Set<String> attributeNames = lr.getRequestedAttributesAsStringSet();

        assertTrue(attributeNames.contains(E_JUSTICE_NATURAL_PERSON_ROLE));
    }

    @Test
    @DisplayName("getLightResponse should populate core fields and all supported attributes when claims exist")
    void getLightResponse_populatesAllFieldsAndAttributes() {
        // Given
        UserInfo userInfo = new UserInfo(new Subject("subject-123"));
        userInfo.setGivenName("Ada");
        userInfo.setFamilyName("Lovelace");
        userInfo.setClaim("pid", "NOR-12345678901");
        userInfo.setClaim("birth_date", "1815-12-10");
        userInfo.setClaim(E_JUSTICE_NATURAL_PERSON_ROLE_CLAIM, "VIP1");

        ILightRequest lightRequest = LightRequest.builder()
                .id("req-1")
                .issuer("issuer")
                .levelOfAssurance("http://eidas.europa.eu/LoA/substantial")
                .relayState("relay-xyz")
                .citizenCountryCode("NO")
                .build();

        // When
        LightResponse lr = specificProxyService.getLightResponse(
                IDPSelector.IDPORTEN,
                userInfo,
                lightRequest,
                no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance.fromString(
                        no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance.EIDAS_LOA_SUBSTANTIAL)
        );

        // Then
        assertNotNull(lr.getId());
        assertEquals("NO", lr.getCitizenCountryCode());
        assertEquals("Issuer", lr.getIssuer());
        assertEquals("relay-xyz", lr.getRelayState());
        assertEquals("req-1", lr.getInResponseToId());
        assertEquals("yes", lr.getConsent());
        assertEquals("subject-123", lr.getSubject());
        assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", lr.getSubjectNameIdFormat());
        assertEquals("http://eidas.europa.eu/LoA/substantial", lr.getLevelOfAssurance());

        // then: status success
        assertNotNull(lr.getStatus());
        assertFalse(lr.getStatus().isFailure());
        assertEquals(eu.eidas.auth.commons.EIDASStatusCode.SUCCESS_URI.getValue(), lr.getStatus().getStatusCode());
        assertEquals("ok", lr.getStatus().getStatusMessage());

        // then: attributes
        Set<String> attrs = lr.getRequestedAttributesAsStringSet();
        assertTrue(attrs.contains(no.idporten.eidas.proxy.service.EidasAttributeNames.FIRST_NAME_EIDAS));
        assertTrue(attrs.contains(no.idporten.eidas.proxy.service.EidasAttributeNames.FAMILY_NAME_EIDAS));
        assertTrue(attrs.contains(no.idporten.eidas.proxy.service.EidasAttributeNames.DATE_OF_BIRTH_EIDAS));
        assertTrue(attrs.contains(no.idporten.eidas.proxy.service.EidasAttributeNames.PID_EIDAS));
        assertTrue(attrs.contains(no.idporten.eidas.proxy.service.EidasAttributeNames.E_JUSTICE_NATURAL_PERSON_ROLE));
    }


}
