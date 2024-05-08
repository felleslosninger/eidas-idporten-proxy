package no.idporten.eidas.proxy.service;

import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.impl.LightRequest;
import no.idporten.eidas.proxy.config.EuProxyProperties;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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


    @Test
    @DisplayName("when buildLightResponse then return LightResponse without validation errors")
    void buildLightResponse() {
        UserInfo userInfo = new UserInfo(new Subject("123456789"));

        // Populate standard claims
        userInfo.setGivenName("John");
        userInfo.setFamilyName("Smith");
        userInfo.setClaim("pid", "123456789");
        userInfo.setClaim("birth_date", "1990-01-01");

        ILightRequest lightRequest = LightRequest.builder()
                .id("myid")
                .issuer("http://euproxy")
                .levelOfAssurance("weirdprefix:eidas-loa-high")
                .relayState("myrelaystate")
                .citizenCountryCode("NO")
                .build();

        OIDCIntegrationService mockOidcIntegrationService = mock(OIDCIntegrationService.class);
        when(mockOidcIntegrationService.getIssuer()).thenReturn("http://myjunit");
        LightResponse lightResponse = specificProxyService.getLightResponse(userInfo, lightRequest, "high");
        assertNotNull(lightResponse);
    }
}
