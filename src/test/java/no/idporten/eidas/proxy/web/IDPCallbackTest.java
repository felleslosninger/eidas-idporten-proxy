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
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

    @Test
    @DisplayName("then when receiving a valid callback that matches an original request, the callback should redirect successfully")
    void callback_shouldRedirectSuccessfully() throws Exception {
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        State state = new State("123q");
        when(authorizationResponse.getState()).thenReturn(state);
        CorrelatedRequestHolder cachedRequest = new CorrelatedRequestHolder(mock(ILightRequest.class), mock(OIDCRequestStateParams.class));
        AuthorizationCode authorizationCode = new AuthorizationCode("authorization_code");
        JWT mockJwt = mock(JWT.class);
        AccessToken mockAccessToken = mock(AccessToken.class);
        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockJwt.getJWTClaimsSet()).thenReturn(JWTClaimsSet.parse("{\"acr\":\"idporten-loa-low\"}"));
        OIDCTokens oidcTokens = new OIDCTokens(mockJwt, mockAccessToken, mockRefreshToken);
        when(mockAccessToken.getValue()).thenReturn("access_token");

        when(specificProxyService.getCachedRequest(state)).thenReturn(cachedRequest);
        when(oidcIntegrationService.getAuthorizationCode(any(AuthorizationResponse.class), eq(cachedRequest))).thenReturn(authorizationCode);
        when(oidcIntegrationService.getToken(any(), any(), any())).thenReturn(oidcTokens);
        UserInfo userInfo = mock(UserInfo.class);
        LightResponse lightResponse = mock(LightResponse.class);
        when(lightResponse.getRelayState()).thenReturn("abc");
        when(oidcIntegrationService.getUserInfo(any())).thenReturn(userInfo);
        when(specificProxyService.idportenAcrListToEidasAcr("idporten-loa-low")).thenReturn(LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW));
        when(specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW))).thenReturn(lightResponse);
        when(specificProxyService.getEuProxyRedirectUri()).thenReturn("http://junit");
        when(specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse)).thenReturn("hello");
        when(specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW))).thenReturn(lightResponse);
        when(specificProxyService.getEuProxyRedirectUri()).thenReturn("http://junit");
        when(specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse)).thenReturn("hello");

        mockMvc.perform(get("http://junit.no/idpcallback?code=123456&state=123q"))
                .andExpect(redirectedUrl("http://junit?token=hello&relayState=abc"));


        verify(specificCommunicationService).putResponse(lightResponse);
    }

    @Test
    @DisplayName("then when receiving a valid callback that matches an original request, but the acr level doesn't match, the callback should not redirect successfully")
    void callbackWithWrongAcr_shouldNotRedirectSuccessfully() throws Exception {
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        ILightRequest mockLightRequest = mock(ILightRequest.class);
        when(mockLightRequest.getLevelsOfAssurance()).thenReturn(List.of(LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_HIGH)));
        State state = new State("123q");
        when(authorizationResponse.getState()).thenReturn(state);
        CorrelatedRequestHolder cachedRequest = new CorrelatedRequestHolder(mockLightRequest, mock(OIDCRequestStateParams.class));
        AuthorizationCode authorizationCode = new AuthorizationCode("authorization_code");
        JWT mockJwt = mock(JWT.class);
        AccessToken mockAccessToken = mock(AccessToken.class);
        RefreshToken mockRefreshToken = mock(RefreshToken.class);
        when(mockJwt.getJWTClaimsSet()).thenReturn(JWTClaimsSet.parse("{\"acr\":\"idporten-loa-low\"}"));
        OIDCTokens oidcTokens = new OIDCTokens(mockJwt, mockAccessToken, mockRefreshToken);
        when(mockAccessToken.getValue()).thenReturn("access_token");

        when(specificProxyService.getCachedRequest(state)).thenReturn(cachedRequest);
        when(oidcIntegrationService.getAuthorizationCode(any(AuthorizationResponse.class), eq(cachedRequest))).thenReturn(authorizationCode);
        when(oidcIntegrationService.getToken(any(), any(), any())).thenReturn(oidcTokens);
        UserInfo userInfo = mock(UserInfo.class);
        LightResponse lightResponse = mock(LightResponse.class);
        when(lightResponse.getRelayState()).thenReturn("abc");
        when(oidcIntegrationService.getUserInfo(any())).thenReturn(userInfo);
        when(specificProxyService.idportenAcrListToEidasAcr("idporten-loa-low")).thenReturn(LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW));
        when(specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW))).thenReturn(lightResponse);
        when(specificProxyService.getEuProxyRedirectUri()).thenReturn("http://junit");
        when(specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse)).thenReturn("hello");
        when(specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), LevelOfAssurance.fromString(ILevelOfAssurance.EIDAS_LOA_LOW))).thenReturn(lightResponse);

        mockMvc.perform(get("http://junit.no/idpcallback?code=123456&state=123q"))
                .andExpect(view().name("it_is_you"));


//        verify(specificCommunicationService).putResponse(lightResponse);
    }

    @Test
    @DisplayName("then when receiving a valid callback that does not match an original request, the callback should redirect with an error message")
    void callback_shouldNotRedirectSuccessfully() throws Exception {
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        State state = new State("123q");
        when(authorizationResponse.getState()).thenReturn(state);
        when(specificProxyService.getCachedRequest(state)).thenReturn(null);

        mockMvc.perform(get("http://junit.no/idpcallback?code=123456&state=123q"))
                .andExpect(view().name("it_is_you"));

    }
}
