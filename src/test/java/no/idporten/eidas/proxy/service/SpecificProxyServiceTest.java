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

import static org.junit.jupiter.api.Assertions.*;
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
        when(oidcIntegrationService.getIssuer()).thenReturn("Issuer");
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
        when(mockOidcIntegrationService.getIssuer()).thenReturn("http://myjunit");
        LightResponse lightResponse = specificProxyService.getLightResponse(userInfo, lightRequest, LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_LOW));
        assertNotNull(lightResponse);
    }

    @Test
    @DisplayName("when a specific exception return LightResponse with detailed error for SpecificProxyException")
    void testGetErrorLightResponseWithSpecificProxyException() {

        SpecificProxyException spex = new SpecificProxyException("Error code", "Error message", mockLightRequest);

        LightResponse result = specificProxyService.getErrorLightResponse(EIDASStatusCode.REQUESTER_URI, spex);

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


        LightResponse result = specificProxyService.getErrorLightResponse(EIDASStatusCode.REQUESTER_URI, ex);

        assertAll("must properly handle generic exceptions and form correct LightResponse",
                () -> assertNotNull(result.getId(), "ID must not be null"),
                () -> assertEquals("NO", result.getCitizenCountryCode(), "CitizenCountryCode must be 'NO'"),
                () -> assertEquals("Issuer", result.getIssuer(), "Issuer must match the expected value"),
                () -> assertNull(result.getRelayState(), "RelayState will be null for generic exceptions"),
                () -> assertEquals("An internal error occurred", result.getStatus().getStatusMessage(), "Status message must reflect an internal error"),
                () -> assertEquals(EIDASStatusCode.REQUESTER_URI.getValue(), result.getStatus().getStatusCode(), "Status code must match the expected value")
        );
    }


}
