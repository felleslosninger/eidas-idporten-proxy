package no.idporten.eidas.proxy.service;

import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @InjectMocks
    SpecificProxyService specificProxyService;

    @BeforeEach
    void setup() {
        when(euProxyProperties.getAcrValueMap()).thenReturn(Map.of(
                "idporten-loa-low", LevelOfAssurance.EIDAS_LOA_LOW,
                "idporten-loa-substantial", LevelOfAssurance.EIDAS_LOA_SUBSTANTIAL,
                "idporten-loa-high", LevelOfAssurance.EIDAS_LOA_HIGH));
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
    void testAcrValueFromEidasToIdportenMapping() {
        List<String> idportenAcr = specificProxyService.eidasAcrListToIdportenAcrList(List.of(
                new LevelOfAssurance("notified", LevelOfAssurance.EIDAS_LOA_LOW),
                new LevelOfAssurance("notified", LevelOfAssurance.EIDAS_LOA_SUBSTANTIAL),
                new LevelOfAssurance("notified", LevelOfAssurance.EIDAS_LOA_HIGH)));
        assertEquals(3, idportenAcr.size());
        assertEquals(List.of("idporten-loa-low", "idporten-loa-substantial", "idporten-loa-high"), idportenAcr);
    }

    @Test
    void testAcrValueFromIdportenToEidasMapping() {
        ILevelOfAssurance eidasAcr = specificProxyService.idportenAcrListToEidasAcr("idporten-loa-high");
        assertNotNull(eidasAcr);
        assertEquals(LevelOfAssurance.EIDAS_LOA_HIGH, eidasAcr.getValue());
    }
}
