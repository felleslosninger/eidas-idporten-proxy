package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.rar.AuthorizationDetail;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import eu.eidas.auth.commons.light.ILightRequest;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.proxy.jwt.ClientAssertionGenerator;
import no.idporten.eidas.proxy.service.IDPSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.*;

import static no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class OIDCIntegrationServiceTest {

    @Mock
    private OIDCProviders oidcProviders;

    @Mock
    private OIDCProvider oidcProvider;
    @Mock
    private OIDCIntegrationProperties oidcIntegrationProperties;
    @Mock
    private OIDCProviderMetadata oidcProviderMetadata;

    @Mock
    private Optional<ClientAssertionGenerator> clientAssertionGenerator;

    @InjectMocks
    private OIDCIntegrationService oidcIntegrationService;

    @BeforeEach
    public void setup() {
        when(oidcProviders.get(IDPSelector.IDPORTEN)).thenReturn(oidcProvider);
        when(oidcProvider.getMetadata()).thenReturn(oidcProviderMetadata);
        when(oidcProvider.getProperties()).thenReturn(oidcIntegrationProperties);
    }


    @Test
    @DisplayName("Test client authentication with private key JWT")
    void testClientAuthenticationPrivateKeyJWT() throws Exception {
        ClientAssertionGenerator mockClientAssertionGenerator = mock(ClientAssertionGenerator.class);
        when(clientAssertionGenerator.isPresent()).thenReturn(true);
        when(clientAssertionGenerator.get()).thenReturn(mockClientAssertionGenerator);
        when(oidcIntegrationProperties.getClientId()).thenReturn("testClientId");
        when(oidcIntegrationProperties.getIssuer()).thenReturn(new URI("https://example.com"));
        when(oidcIntegrationProperties.getClientAuthMethod()).thenReturn("private_key_jwt");
        when(mockClientAssertionGenerator.create(IDPSelector.IDPORTEN)).thenReturn(mock(PrivateKeyJWT.class));

        ClientAuthentication clientAuth = oidcIntegrationService.clientAuthentication(IDPSelector.IDPORTEN);
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
        AuthenticationRequest authRequest = oidcIntegrationService.createAuthenticationRequest(IDPSelector.IDPORTEN, codeVerifier, acrValues, serviceProviderCountryCode);

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
                () -> assertEquals(EIDAS_DISPLAY_NAME, authRequest.getCustomParameter(ONBEHALFOF_NAME).getFirst(), "onbehalfof_name custom parameter should match")
        );
    }

    // --- Tests for eJustice role mapping from authorization_details ---
    private static JWTClaimsSet claimsWithAuthorizationDetails(List<Map<String, Object>> adList) {
        return new JWTClaimsSet.Builder()
                .claim(AUTHORIZATION_DETAILS_CLAIM, adList)
                .build();
    }

    @Test
    @DisplayName("the authorization details claim must be parsed correcty")
    void parsesAuthorizationDetailsClaim() throws Exception {
        String claim = """
                {"sub":"xxx","amr":["BankID"],"iss":"https://ansattporten.dev","pid":"05910298382","locale":"nb","nonce":"KSTNlmgUxOYyUqqgwZyr0lPWGxVDNzUOmsRMTdX5vjs","aud":"eidas-proxy-client-ansattporten-docker","acr":"high","authorization_details":[{"authorized_parties":[{"orgno":{"authority":"iso6523-actorid-upis","ID":"0192:312702495"},"resource":"boris---vip1-tilgang","name":"AKADEMISK STANDHAFTIG TIGER AS","unit_type":"AS"}],"resource":"urn:altinn:resource:boris---vip1-tilgang","type":"ansattporten:altinn:resource","resource_name":"BORIS - VIP1 tilgang"}],"auth_time":1765294441,"name":"STOLT EFFEKTIV PARASOLL","exp":1765294565,"iat":1765294445,"jti":"hTolsTXz-TE"}
                """;
        JWTClaimsSet claims = JWTClaimsSet.parse(claim);
        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);
        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        assertEquals(1, parsed.size());
    }

    @Test
    @DisplayName("should set eJustice role to VIP1 when VIP1 resource is present")
    void setsVip1WhenVip1ResourcePresent() throws Exception {
        Map<String, Object> ad = new HashMap<>();
        ad.put("type", "ansattporten:altinn:resource");
        ad.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG);
        JWTClaimsSet claims = claimsWithAuthorizationDetails(List.of(ad));

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);

        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        String role = svc.getEJusticeRoleClaim(parsed);

        assertEquals("VIP1", role);
    }

    @Test
    @DisplayName("should set eJustice role to VIP2 when VIP2 resource is present")
    void setsVip2WhenVip2ResourcePresent() throws Exception {
        Map<String, Object> ad = new HashMap<>();
        ad.put("type", "ansattporten:altinn:resource");
        ad.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG);
        JWTClaimsSet claims = claimsWithAuthorizationDetails(List.of(ad));

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);

        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        String role = svc.getEJusticeRoleClaim(parsed);

        assertEquals("VIP2", role);
    }

    @Test
    @DisplayName("should prefer VIP1 when both VIP1 and VIP2 resources are present")
    void prefersVip1WhenBothPresent() throws Exception {
        Map<String, Object> ad1 = new HashMap<>();
        ad1.put("type", "ansattporten:altinn:resource");
        ad1.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG);

        Map<String, Object> ad2 = new HashMap<>();
        ad2.put("type", "ansattporten:altinn:resource");
        ad2.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG);

        JWTClaimsSet claims = claimsWithAuthorizationDetails(List.of(ad1, ad2));

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);

        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        String role = svc.getEJusticeRoleClaim(parsed);

        assertEquals("VIP1", role);
    }

    @Test
    @DisplayName("should return null when no relevant Altinn resource is present")
    void returnsNullWhenNoRelevantResource() throws Exception {
        Map<String, Object> ad = new HashMap<>();
        ad.put("type", "ansattporten:altinn:resource");
        ad.put(RESOURCE, "urn:altinn:resource:unrelated");
        JWTClaimsSet claims = claimsWithAuthorizationDetails(List.of(ad));

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);

        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        String role = svc.getEJusticeRoleClaim(parsed);

        assertNull(role);
    }
}


