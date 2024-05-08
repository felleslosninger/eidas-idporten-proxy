package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import eu.eidas.auth.commons.EidasParameterKeys;
import io.lettuce.core.RedisConnectionException;
import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import no.idporten.eidas.proxy.lightprotocol.messages.RequestedAttribute;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(ProxyServiceRequestController.class)
@DisplayName("When calling the ProxyServiceRequestController")
class ProxyServiceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecificCommunicationService specificCommunicationService;

    @MockBean
    private SpecificProxyService specificProxyService;

    @MockBean
    private EidasCacheProperties eidasCacheProperties;

    @MockBean
    private OIDCIntegrationService oidcIntegrationService;

    private final static String lightTokenId = "mockedLightTokenId";

    @BeforeAll
    static void setup() throws SpecificCommunicationException {
        String tokenBase64 = "mockedTokenBase64";
        mockStatic(BinaryLightTokenHelper.class);
        when(BinaryLightTokenHelper.getBinaryToken(any(HttpServletRequest.class), eq(EidasParameterKeys.TOKEN.toString()))).thenReturn(tokenBase64);
        when(BinaryLightTokenHelper.getBinaryLightTokenId(eq(tokenBase64), any(), any())).thenReturn(lightTokenId);

    }

    @Test
    @DisplayName("then if there is a valid lightrequest return redirect to authorization endpoint")
    void testValidLightRequest() throws Exception {
        LightRequest lightRequest = LightRequest.builder()
                .citizenCountryCode("NO")
                .id("123")
                .issuer("issuer")
                .providerName("providerName")
                .levelOfAssurance(new LevelOfAssurance("notified", LevelOfAssurance.EIDAS_LOA_LOW))
                .spType("spType")
                .spCountryCode("spCountryCode")
                .relayState("relayState")
                .requestedAttributes(List.of(new RequestedAttribute(null, "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName")))
                .build();

        when(specificCommunicationService.getAndRemoveRequest(any(String.class), any())).thenReturn(lightRequest);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest.Builder(new URI("http://example.com"), new ClientID("123")).build();
        when(oidcIntegrationService.pushedAuthorizationRequest(authenticationRequest)).thenReturn(new URI("http://redirect-url.com"));
        when(oidcIntegrationService.getAuthorizationEndpoint()).thenReturn(new URI("http://authorization-endpoint.com"));

        when(specificProxyService.translateNodeRequest(lightRequest)).thenReturn(authenticationRequest);

        mockMvc.perform(post("/ProxyServiceRequest"))
                .andExpect(redirectedUrl("http://authorization-endpoint.com?client_id=123&request_uri=http%3A%2F%2Fredirect-url.com"));
    }

    @Test
    @DisplayName("then if there is an ivalid lightrequest return an error message")
    void testInvalidLightRequest() throws Exception {
        LightRequest lightRequest = LightRequest.builder()
                .citizenCountryCode("SE")
                .id("123")
                .issuer("issuer")
                .providerName("providerName")
                .levelOfAssurance(new LevelOfAssurance("notified", LevelOfAssurance.EIDAS_LOA_LOW))
                .spType("spType")
                .spCountryCode("spCountryCode")
                .relayState("relayState")
                .requestedAttributes(List.of(new RequestedAttribute(null, "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName")))
                .build();

        when(specificCommunicationService.getAndRemoveRequest(any(String.class), any())).thenReturn(lightRequest);

        mockMvc.perform(post("/ProxyServiceRequest"))
                .andExpect(forwardedUrl("it_is_you"));
    }

    @Test
    @DisplayName("then if there is a missing lightrequest return error message")
    void testMissingLightRequest() throws Exception {

        when(specificCommunicationService.getAndRemoveRequest(any(String.class), any())).thenReturn(null);

        mockMvc.perform(post("/ProxyServiceRequest"))
                .andExpect(forwardedUrl("it_is_you"));
    }

    @Test
    @DisplayName("then if there is an internal error return error message")
    void testInternalError() throws Exception {

        when(specificCommunicationService.getAndRemoveRequest(any(String.class), any())).thenThrow(new RedisConnectionException("Internal error"));

        mockMvc.perform(post("/ProxyServiceRequest"))
                .andExpect(forwardedUrl("it_is_us"));
    }
}
