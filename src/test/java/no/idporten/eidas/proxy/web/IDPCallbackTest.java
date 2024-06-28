package no.idporten.eidas.proxy.web;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.logging.AuditService;
import no.idporten.eidas.proxy.service.LevelOfAssuranceHelper;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(IDPCallback.class)
@DisplayName("When receiving a callback from the IDP")
class IDPCallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecificProxyService specificProxyService;

    @MockBean
    private OIDCIntegrationService oidcIntegrationService;

    @MockBean
    private SpecificCommunicationService specificCommunicationService;
    @MockBean
    private AuditService auditService;

    @MockBean
    private LevelOfAssuranceHelper levelOfAssuranceHelper;
    private ILightRequest mockLightRequest;
    private State state;

    private ILevelOfAssurance levelOfAssurance = LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW);

    @BeforeEach
    void setup() {
        when(levelOfAssuranceHelper.hasValidAcrLevel(any(), any())).thenReturn(true);
        when(levelOfAssuranceHelper.idportenAcrListToEidasAcr("idporten-loa-low")).thenReturn(levelOfAssurance);
        when(specificProxyService.getEuProxyRedirectUri()).thenReturn("http://junit");
        mockLightRequest = mock(ILightRequest.class);
        state = new State("123q");
        when(mockLightRequest.getRelayState()).thenReturn("abc");
        CorrelatedRequestHolder correlatedRequestHolder = new CorrelatedRequestHolder(mockLightRequest, mock(OIDCRequestStateParams.class));
        when(specificProxyService.getCachedRequest(state)).thenReturn(correlatedRequestHolder);
        when(specificProxyService.createStoreBinaryLightTokenResponseBase64(any(LightResponse.class))).thenReturn("hello");
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        when(authorizationResponse.getState()).thenReturn(state);
        LightResponse lightResponse = mock(LightResponse.class);
        when(specificProxyService.getErrorLightResponse(any(), any())).thenReturn(lightResponse);

    }

    @AfterEach
    void end() {
        verify(specificProxyService).getCachedRequest(any(State.class));
    }

    @Test
    @DisplayName("then when receiving a valid callback that matches an original request, the callback should redirect successfully")
    void callback_shouldRedirectSuccessfully() throws Exception {

        AuthorizationCode authorizationCode = new AuthorizationCode("authorization_code");
        JWT mockJwt = mock(JWT.class);
        AccessToken mockAccessToken = mock(AccessToken.class);
        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockJwt.getJWTClaimsSet()).thenReturn(JWTClaimsSet.parse("{\"acr\":\"idporten-loa-low\"}"));
        OIDCTokens oidcTokens = new OIDCTokens(mockJwt, mockAccessToken, mockRefreshToken);
        when(mockAccessToken.getValue()).thenReturn("access_token");


        when(oidcIntegrationService.getAuthorizationCode(any(AuthorizationResponse.class), any(CorrelatedRequestHolder.class))).thenReturn(authorizationCode);
        when(oidcIntegrationService.getToken(any(), any(), any())).thenReturn(oidcTokens);
        UserInfo userInfo = mock(UserInfo.class);

        LightResponse lightResponse = mock(LightResponse.class);
        when(lightResponse.getRelayState()).thenReturn("abc");
        when(oidcIntegrationService.getUserInfo(any())).thenReturn(userInfo);

        when(specificProxyService.getLightResponse(userInfo, mockLightRequest, levelOfAssurance)).thenReturn(lightResponse);
        when(specificProxyService.getEuProxyRedirectUri()).thenReturn("http://junit");

        mockMvc.perform(get("http://junit.no/idpcallback?code=123456&state=123q"))
                .andExpect(redirectedUrl("http://junit?token=hello"));

        verify(auditService).auditLightResponse(lightResponse);
    }

    @Test
    @DisplayName("then when receiving a valid callback that matches an original request, but the acr level doesn't match, the callback should not redirect successfully")
    void callbackWithWrongAcr_shouldNotRedirectSuccessfully() throws Exception {

        when(mockLightRequest.getLevelsOfAssurance()).thenReturn(List.of(LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_HIGH)));

        AuthorizationCode authorizationCode = new AuthorizationCode("authorization_code");
        JWT mockJwt = mock(JWT.class);
        AccessToken mockAccessToken = mock(AccessToken.class);
        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockJwt.getJWTClaimsSet()).thenReturn(JWTClaimsSet.parse("{\"acr\":\"idporten-loa-low\"}"));
        OIDCTokens oidcTokens = new OIDCTokens(mockJwt, mockAccessToken, mockRefreshToken);
        when(mockAccessToken.getValue()).thenReturn("access_token");


        when(oidcIntegrationService.getAuthorizationCode(any(AuthorizationResponse.class), any(CorrelatedRequestHolder.class))).thenReturn(authorizationCode);
        when(oidcIntegrationService.getToken(any(), any(), any())).thenReturn(oidcTokens);
        UserInfo userInfo = mock(UserInfo.class);
        LightResponse lightResponse = mock(LightResponse.class);
        when(lightResponse.getRelayState()).thenReturn("abc");
        when(oidcIntegrationService.getUserInfo(any())).thenReturn(userInfo);
        when(specificProxyService.getLightResponse(userInfo, mockLightRequest, levelOfAssurance)).thenReturn(lightResponse);
        when(specificProxyService.getEuProxyRedirectUri()).thenReturn("http://junit");

        when(specificProxyService.getLightResponse(userInfo, mockLightRequest, levelOfAssurance)).thenReturn(lightResponse);

        mockMvc.perform(get("http://junit.no/idpcallback?code=123456&state=123q"))
                .andExpect(redirectedUrl("http://junit?token=hello"));
        verify(auditService).auditLightResponse(lightResponse);

    }

    @Test
    @DisplayName("then when receiving a valid callback that does not match an original request, the callback should redirect with an error message")
    void callback_shouldNotRedirectSuccessfully() throws Exception {

        when(specificProxyService.getCachedRequest(state)).thenReturn(null);

        mockMvc.perform(get("http://junit.no/idpcallback?code=123456&state=123q"))
                .andExpect(redirectedUrl("http://junit?token=hello"));
        verify(auditService).auditLightResponse(any());
    }
}
