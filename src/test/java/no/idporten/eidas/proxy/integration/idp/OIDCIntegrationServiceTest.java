package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import eu.eidas.auth.commons.light.ILightRequest;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.proxy.jwt.ClientAssertionGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class OIDCIntegrationServiceTest {
    @Mock
    private OIDCIntegrationProperties properties;
    @Mock
    private OIDCProviderMetadata providerMetadata;

    @Mock
    private Optional<ClientAssertionGenerator> clientAssertionGenerator;

    @InjectMocks
    private OIDCIntegrationService oidcIntegrationService;


    @Test
    @DisplayName("Test client authentication with private key JWT")
    void testClientAuthenticationPrivateKeyJWT() throws Exception {
        ClientAssertionGenerator mockClientAssertionGenerator = mock(ClientAssertionGenerator.class);
        when(clientAssertionGenerator.isPresent()).thenReturn(true);
        when(clientAssertionGenerator.get()).thenReturn(mockClientAssertionGenerator);
        when(properties.getClientId()).thenReturn("testClientId");
        when(properties.getIssuer()).thenReturn(new URI("https://example.com"));
        when(properties.getClientAuthMethod()).thenReturn("private_key_jwt");
        when(mockClientAssertionGenerator.create()).thenReturn(mock(PrivateKeyJWT.class));

        ClientAuthentication clientAuth = oidcIntegrationService.clientAuthentication();
        assertInstanceOf(PrivateKeyJWT.class, clientAuth);
    }

    @Test
    @DisplayName("Test getting authorization code")
    void testGetAuthorizationCode() {
        AuthorizationSuccessResponse successResponse = mock(AuthorizationSuccessResponse.class);
        when(successResponse.getAuthorizationCode()).thenReturn(new AuthorizationCode("authorization_code"));

        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        when(authorizationResponse.indicatesSuccess()).thenReturn(true);
        when(authorizationResponse.toSuccessResponse()).thenReturn(successResponse);

        CorrelatedRequestHolder cachedRequest = mock(CorrelatedRequestHolder.class);
        when(cachedRequest.getiLightRequest()).thenReturn(mock(ILightRequest.class));
        when(cachedRequest.getAuthenticationRequest()).thenReturn(mock(OIDCRequestStateParams.class));

        assertDoesNotThrow(() -> oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest));

    }

    @Test
    @DisplayName("Test getting authorization code with error response")
    void testGetAuthorizationCodeError() {

        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        when(authorizationResponse.indicatesSuccess()).thenReturn(false);

        CorrelatedRequestHolder cachedRequest = mock(CorrelatedRequestHolder.class);
        when(cachedRequest.getiLightRequest()).thenReturn(mock(ILightRequest.class));
        when(cachedRequest.getAuthenticationRequest()).thenReturn(mock(OIDCRequestStateParams.class));

        AuthorizationErrorResponse errorResponse = mock(AuthorizationErrorResponse.class);
        when(errorResponse.getErrorObject()).thenReturn(new com.nimbusds.oauth2.sdk.ErrorObject("error"));
        when(authorizationResponse.indicatesSuccess()).thenReturn(false);

        assertThrows(OAuthException.class, () -> oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest));
    }
}


