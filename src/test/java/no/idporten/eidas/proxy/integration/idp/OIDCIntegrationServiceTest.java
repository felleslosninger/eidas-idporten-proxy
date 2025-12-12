package no.idporten.eidas.proxy.integration.idp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
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
import static no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties.RESOURCE;
import static no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties.TYPE;
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

    @Test
    @DisplayName("the authorization details claim must be parsed correctly")
    void parsesAuthorizationDetailsClaim() throws Exception {
        net.minidev.json.JSONObject ad = new net.minidev.json.JSONObject();
        ad.put(TYPE, "altinn");
        ad.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG);
        JWTClaimsSet claims = claimsWithAuthorizationDetails(List.of(ad));

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);
        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        assertEquals(1, parsed.size());
    }

    @Test
    @DisplayName("should set eJustice role to VIP1 when VIP1 resource is present")
    void setsVip1WhenVip1ResourcePresent() throws Exception {
        net.minidev.json.JSONObject ad = new net.minidev.json.JSONObject();
        ad.put(TYPE, "altinn");
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
        net.minidev.json.JSONObject ad = new net.minidev.json.JSONObject();
        ad.put(TYPE, "altinn");
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
        net.minidev.json.JSONObject ad1 = new net.minidev.json.JSONObject();
        ad1.put(TYPE, "altinn");
        ad1.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG);

        net.minidev.json.JSONObject ad2 = new net.minidev.json.JSONObject();
        ad2.put(TYPE, "altinn");
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
        net.minidev.json.JSONObject ad = new net.minidev.json.JSONObject();
        ad.put(TYPE, "altinn");
        ad.put(RESOURCE, "urn:altinn:resource:unrelated");
        JWTClaimsSet claims = claimsWithAuthorizationDetails(List.of(ad));

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);

        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        String role = svc.getEJusticeRoleClaim(parsed);

        assertNull(role);
    }

    @Test
    @DisplayName("should return null when no AuthorizationDetails is present")
    void returnsNullWhenNoAuthorizationDetails() throws Exception {

        JWTClaimsSet claims = claimsWithAuthorizationDetails(null);

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);

        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        String role = svc.getEJusticeRoleClaim(parsed);

        assertNull(role);
    }

    @Test
    @DisplayName("parses authorization_details from List<LinkedTreeMap> and extracts VIP1 role")
    void parsesFromLinkedTreeMapList() throws Exception {
        List<Map<String, Object>> adList = new ArrayList<>();
        Map<String, Object> m = new LinkedTreeMap<>();
        m.put(TYPE, "altinn");
        m.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG);
        adList.add(m);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim(AUTHORIZATION_DETAILS_CLAIM, adList)
                .build();

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);
        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        assertEquals(1, parsed.size());
        String role = svc.getEJusticeRoleClaim(parsed);
        assertEquals("VIP1", role);
    }

    @Test
    @DisplayName("parses authorization_details from List<LinkedHashMap> and extracts VIP1 role")
    void parsesFromLinkedHashMapList() throws Exception {
        List<Map<String, Object>> adList = new ArrayList<>();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(TYPE, "altinn");
        m.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG);
        adList.add(m);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim(AUTHORIZATION_DETAILS_CLAIM, adList)
                .build();

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);
        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        assertEquals(1, parsed.size());
        String role = svc.getEJusticeRoleClaim(parsed);
        assertEquals("VIP1", role);
    }

    @Test
    @DisplayName("parses authorization_details from single Map object and extracts VIP1 role; validation passes when allowed")
    void parsesFromSingleMapObjectAndValidates() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(TYPE, "altinn");
        m.put(RESOURCE, URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim(AUTHORIZATION_DETAILS_CLAIM, m)
                .build();

        OIDCIntegrationService svc = new OIDCIntegrationService(null, Optional.empty(), null);
        List<AuthorizationDetail> parsed = svc.getAuthorizationDetailsClaim(claims);
        assertEquals(1, parsed.size());
        String role = svc.getEJusticeRoleClaim(parsed);
        assertEquals("VIP1", role);

        // Setup properties to allow validation to pass for Ansattporten
        var props = new OIDCIntegrationProperties();
        props.setClientAuthMethod("client_secret_basic");
        props.setClientSecret("secret");
        props.setClientId("client-id");
        props.setIssuer(new URI("https://issuer.example"));
        props.setRedirectUri(new URI("https://client.example/cb"));
        props.setScopes(Set.of("openid"));
        props.setAuthorizationDetails(List.of(
                cfgAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG)
        ));
        props.afterPropertiesSet();

        OIDCProvider apProvider = mock(OIDCProvider.class);
        when(apProvider.getProperties()).thenReturn(props);
        when(oidcProviders.get(IDPSelector.ANSATTPORTEN)).thenReturn(apProvider);
        oidcIntegrationService.validateAuthorizationDetailsClaims(parsed);

        assertDoesNotThrow(() -> {
            try {
                oidcIntegrationService.validateAuthorizationDetailsClaims(parsed);
            } catch (OAuthException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static JWTClaimsSet claimsWithAuthorizationDetails(List<net.minidev.json.JSONObject> adList) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

        if (adList != null) {
            builder.claim(AUTHORIZATION_DETAILS_CLAIM, adList);
        }
        return builder.build();
    }

    // --- Validation tests for type:resource pairs ---

    @Test
    @DisplayName("validateAuthorizationDetailsClaims: passes when all pairs are allowed")
    void validateAuthorizationDetailsClaims_allAllowed() throws Exception {
        // Prepare real properties with allowed pairs for Ansattporten
        var props = new OIDCIntegrationProperties();
        props.setClientAuthMethod("client_secret_basic");
        props.setClientSecret("secret");
        props.setClientId("client-id");
        props.setIssuer(new URI("https://issuer.example"));
        props.setRedirectUri(new URI("https://client.example/cb"));
        props.setScopes(Set.of("openid"));
        props.setAuthorizationDetails(List.of(
                cfgAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG),
                cfgAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG)
        ));
        props.afterPropertiesSet();

        OIDCProvider apProvider = mock(OIDCProvider.class);
        when(apProvider.getProperties()).thenReturn(props);
        when(oidcProviders.get(IDPSelector.ANSATTPORTEN)).thenReturn(apProvider);

        // Build claim list with allowed pairs
        List<AuthorizationDetail> claimAds = List.of(
                nimbusAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG),
                nimbusAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG)
        );

        assertDoesNotThrow(() -> {
            try {
                oidcIntegrationService.validateAuthorizationDetailsClaims(claimAds);
            } catch (OAuthException e) {
                // unwrap and rethrow as RuntimeException to let assertDoesNotThrow catch
                throw new RuntimeException(e.getCause());
            }
        });
    }

    @Test
    @DisplayName("validateAuthorizationDetailsClaims: throws when any pair is not allowed")
    void validateAuthorizationDetailsClaims_throwsOnUnknownPair() throws Exception {
        var props = new OIDCIntegrationProperties();
        props.setClientAuthMethod("client_secret_basic");
        props.setClientSecret("secret");
        props.setClientId("client-id");
        props.setIssuer(new URI("https://issuer.example"));
        props.setRedirectUri(new URI("https://client.example/cb"));
        props.setScopes(Set.of("openid"));
        props.setAuthorizationDetails(List.of(
                cfgAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG)
        ));
        props.afterPropertiesSet();

        OIDCProvider apProvider = mock(OIDCProvider.class);
        when(apProvider.getProperties()).thenReturn(props);
        when(oidcProviders.get(IDPSelector.ANSATTPORTEN)).thenReturn(apProvider);

        List<AuthorizationDetail> claimAds = List.of(
                nimbusAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG),
                nimbusAd("altinn", "urn:altinn:resource:unknown")
        );

        try {
            oidcIntegrationService.validateAuthorizationDetailsClaims(claimAds);
        } catch (Exception ex) {
            assertInstanceOf(OAuthException.class, ex.getCause());
        }
    }

    @Test
    @DisplayName("validateAuthorizationDetailsClaims: skips validation when config is empty")
    void validateAuthorizationDetailsClaims_skipsWhenConfigEmpty() throws Exception {
        var props = new OIDCIntegrationProperties();
        props.setClientAuthMethod("client_secret_basic");
        props.setClientSecret("secret");
        props.setClientId("client-id");
        props.setIssuer(new URI("https://issuer.example"));
        props.setRedirectUri(new URI("https://client.example/cb"));
        props.setScopes(Set.of("openid"));
        props.setAuthorizationDetails(List.of()); // empty
        props.afterPropertiesSet();

        OIDCProvider apProvider = mock(OIDCProvider.class);
        when(apProvider.getProperties()).thenReturn(props);
        when(oidcProviders.get(IDPSelector.ANSATTPORTEN)).thenReturn(apProvider);

        List<AuthorizationDetail> claimAds = List.of(
                nimbusAd("altinn", URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG)
        );

        assertDoesNotThrow(() -> {
            try {
                oidcIntegrationService.validateAuthorizationDetailsClaims(claimAds);
            } catch (OAuthException e) {
                // unwrap and rethrow as RuntimeException to let assertDoesNotThrow catch
                throw new RuntimeException(e.getCause());
            }
        });
    }

    private static AuthorizationDetail nimbusAd(String type, String resource) throws com.nimbusds.oauth2.sdk.ParseException {
        net.minidev.json.JSONObject jo = new net.minidev.json.JSONObject();
        jo.put(TYPE, type);
        jo.put(RESOURCE, resource);
        return AuthorizationDetail.parse(jo);
    }

    private static no.idporten.sdk.oidcserver.protocol.AuthorizationDetail cfgAd(String type, String resource) {
        Map<String, Object> map = Map.of(TYPE, type, RESOURCE, resource);
        return new ObjectMapper().convertValue(map, no.idporten.sdk.oidcserver.protocol.AuthorizationDetail.class);
    }
}


