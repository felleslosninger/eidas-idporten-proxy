package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.claims.ACR;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class OIDCIntegrationServiceTest {
    @Mock
    private OIDCIntegrationProperties oidcIntegrationProperties;
    @Mock
    private OIDCProviderMetadata oidcProviderMetadata;

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
        when(oidcIntegrationProperties.getClientId()).thenReturn("testClientId");
        when(oidcIntegrationProperties.getIssuer()).thenReturn(new URI("https://example.com"));
        when(oidcIntegrationProperties.getClientAuthMethod()).thenReturn("private_key_jwt");
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

    @Test
    void testCreateAuthenticationRequest() throws Exception {
        // Mocking the required objects and their behavior
        CodeVerifier codeVerifier = new CodeVerifier();
        List<String> acrValues = List.of("no-notified-high");
        String serviceProviderCountryCode = "SE";

        when(oidcIntegrationProperties.getScopes()).thenReturn(Set.of("openid", "eidas:mds"));
        when(oidcIntegrationProperties.getClientId()).thenReturn("client-id");
        when(oidcIntegrationProperties.getRedirectUri()).thenReturn(new URI("https://example.com/callback"));
        when(oidcProviderMetadata.getPushedAuthorizationRequestEndpointURI()).thenReturn(new URI("https://example.com/authorize"));

        // Call the method under test
        AuthenticationRequest authRequest = oidcIntegrationService.createAuthenticationRequest(codeVerifier, acrValues, serviceProviderCountryCode);

        // Using assertAll to group all assertions together
        assertAll("AuthenticationRequest validation",
                () -> assertNotNull(authRequest, "AuthenticationRequest should not be null"),
                () -> assertEquals(ResponseType.CODE, authRequest.getResponseType(), "ResponseType should be CODE"),
                () -> assertEquals(new ClientID("client-id"), authRequest.getClientID(), "ClientID should match"),
                () -> assertEquals(new URI("https://example.com/callback"), authRequest.getRedirectionURI(), "Redirection URI should match"),
                () -> assertEquals(List.of(new ACR("no-notified-high")), authRequest.getACRValues(), "ACR values should match"),
                () -> assertNotNull(authRequest.getState(), "State should not be null"),
                () -> assertNotNull(authRequest.getNonce(), "Nonce should not be null"),
                () -> assertEquals(CodeChallengeMethod.S256, authRequest.getCodeChallengeMethod(), "CodeChallengeMethod should be S256"),
                () -> assertEquals(serviceProviderCountryCode, authRequest.getCustomParameter(ONBEHALFOF).getFirst(), "onbehalfof custom parameter should match"),
                () -> assertEquals(DIGDIR_ORGNO, authRequest.getCustomParameter(ONBEHALFOF_ORGNO).getFirst(), "onbehalfof_orgno custom parameter should match"),
                () -> assertEquals(IDPORTEN_EIDAS_PROXY, authRequest.getCustomParameter(ONBEHALFOF_NAME).getFirst(), "onbehalfof_name custom parameter should match")
        );
    }
}


