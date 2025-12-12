package no.idporten.eidas.proxy.integration.idp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.rar.AuthorizationDetail;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.jwt.ClientAssertionGenerator;
import no.idporten.eidas.proxy.logging.AuditService;
import no.idporten.eidas.proxy.service.IDPSelector;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties.RESOURCE;

@Service
@RequiredArgsConstructor
@Slf4j
public class OIDCIntegrationService {

    //trusted onbehalfof parameters
    protected static final String ONBEHALFOF = "onbehalfof";
    protected static final String ONBEHALFOF_ORGNO = "onbehalfof_orgno";
    protected static final String ONBEHALFOF_NAME = "onbehalfof_name";
    protected static final String DIGDIR_ORGNO = "991825827";
    protected static final String EIDAS_DISPLAY_NAME = "eIDAS";
    protected static final String AUTHORIZATION_DETAILS_CLAIM = "authorization_details";
    public static final String E_JUSTICE_NATURAL_PERSON_ROLE_CLAIM = "eJusticeNaturalPersonRole";
    public static final String URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG = "urn:altinn:resource:boris---vip1-tilgang";
    public static final String URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG = "urn:altinn:resource:boris---vip2-tilgang";


    private final OIDCProviders oidcProviders;
    private final Optional<ClientAssertionGenerator> jwtGrantGenerator;
    private final AuditService auditService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthenticationRequest createAuthenticationRequest(String idp, CodeVerifier codeVerifier, List<String> acrValues, String serviceProviderCountryCode) {
        OIDCIntegrationProperties oidcIntegrationProperties = this.oidcProviders.get(idp).getProperties();
        OIDCProviderMetadata oidcProviderMetadata = this.oidcProviders.get(idp).getMetadata();
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(ResponseType.CODE,
                new Scope(oidcIntegrationProperties.getScopes().toArray(new String[0])),
                new ClientID(oidcIntegrationProperties.getClientId()),
                oidcIntegrationProperties.getRedirectUri());
        builder.endpointURI(oidcProviderMetadata.getPushedAuthorizationRequestEndpointURI())
                .acrValues(acrValues.stream().map(ACR::new).toList())
                .state(new com.nimbusds.oauth2.sdk.id.State())
                .nonce(new Nonce())
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .customParameter(ONBEHALFOF, serviceProviderCountryCode)
                .customParameter(ONBEHALFOF_ORGNO, DIGDIR_ORGNO)
                .customParameter(ONBEHALFOF_NAME, EIDAS_DISPLAY_NAME)
        ;

        //send authorization_details if configured on oidcprovider
        if (CollectionUtils.isNotEmpty(this.oidcProviders.get(idp).getProperties().getAuthorizationDetails())) {
            builder.authorizationDetails(toNimbusAuthorizationDetails(idp));
        }
        return builder.build();

    }

    private List<com.nimbusds.oauth2.sdk.rar.AuthorizationDetail> toNimbusAuthorizationDetails(String idp) {
        java.util.List<AuthorizationDetail> rarDetails = new java.util.ArrayList<>();
        for (no.idporten.sdk.oidcserver.protocol.AuthorizationDetail ad : this.oidcProviders.get(idp).getProperties().getAuthorizationDetails()) {
            // Convert our DTO to a JSON object understood by Nimbus RAR AuthorizationDetail
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.convertValue(ad, Map.class);
            net.minidev.json.JSONObject jsonObject = new net.minidev.json.JSONObject();
            jsonObject.putAll(map);
            try {
                AuthorizationDetail rarAd = AuthorizationDetail.parse(jsonObject);
                rarDetails.add(rarAd);

            } catch (com.nimbusds.oauth2.sdk.ParseException e) {
                throw new IllegalArgumentException("Invalid authorization_details element: " + jsonObject, e);
            }
        }
        return rarDetails;
    }

    public URI pushedAuthorizationRequest(String idp, AuthenticationRequest authenticationRequest) throws IOException, ParseException {
        OIDCIntegrationProperties oidcIntegrationProperties = this.oidcProviders.get(idp).getProperties();
        OIDCProviderMetadata oidcProviderMetadata = this.oidcProviders.get(idp).getMetadata();
        final ClientAuthentication clientAuth = new ClientSecretBasic(new ClientID(oidcIntegrationProperties.getClientId()), new Secret(oidcIntegrationProperties.getClientSecret()));
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(
                oidcProviderMetadata.getPushedAuthorizationRequestEndpointURI(), clientAuth, authenticationRequest);
        auditService.auditIDPParRequest(pushedAuthorizationRequest, null, idp);

        HTTPResponse httpResponse = pushedAuthorizationRequest.toHTTPRequest().send();
        PushedAuthorizationResponse response = PushedAuthorizationResponse.parse(httpResponse);

        if (!response.indicatesSuccess()) {
            ErrorObject errorObject = response.toErrorResponse().getErrorObject();
            log.warn("PAR request failed: HttpStatusCode={}, error_code={}, description={}", errorObject.getHTTPStatusCode(), errorObject.getCode(), errorObject.getDescription());
            throw new OAuthException("PAR request failed: " + errorObject.getDescription());
        }

        PushedAuthorizationSuccessResponse successResponse = response.toSuccessResponse();

        return successResponse.getRequestURI();
    }


    public AuthorizationCode getAuthorizationCode(AuthorizationResponse authorizationResponse, CorrelatedRequestHolder cachedRequest) throws ParseException {

        if (cachedRequest == null) {
            log.error("No request found for state {}", authorizationResponse.getState());
            throw new OAuthException("No request found for state " + authorizationResponse.getState());
        }
        if (!authorizationResponse.indicatesSuccess()) {
            String errorDescription = "not returned";
            if (authorizationResponse.toErrorResponse() != null) {
                AuthorizationErrorResponse errorResponse = AuthorizationErrorResponse.parse(authorizationResponse.toErrorResponse().toURI());
                errorDescription = errorResponse.getErrorObject().getDescription();
            }
            throw new OAuthException("Authorization response indicates failure %s".formatted(errorDescription));
        }

        AuthorizationSuccessResponse successResponse = authorizationResponse.toSuccessResponse();
        return successResponse.getAuthorizationCode();
    }

    public OIDCTokens getToken(String idp, AuthorizationCode code, CodeVerifier codeVerifier, Nonce nonce) throws IOException, ParseException, BadJOSEException, JOSEException {
        OIDCIntegrationProperties oidcIntegrationProperties = this.oidcProviders.get(idp).getProperties();
        OIDCProviderMetadata oidcProviderMetadata = this.oidcProviders.get(idp).getMetadata();
        URI callback = oidcIntegrationProperties.getRedirectUri();
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback, codeVerifier);

        TokenRequest request = new TokenRequest(oidcProviderMetadata.getTokenEndpointURI(), clientAuthentication(idp), codeGrant, null);

        TokenResponse response = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse errorResponse = response.toErrorResponse();
            throw new OAuthException("Token request failed: " + errorResponse.getErrorObject());
        }
        OIDCTokenResponse successResponse = (OIDCTokenResponse) response.toSuccessResponse();
        // Validate ID Token using the validator associated with the selected IdP
        this.oidcProviders.get(idp).getIdTokenValidator().validate(successResponse.getOIDCTokens().getIDToken(), nonce);
        return successResponse.getOIDCTokens();
    }

    public UserInfo getUserInfo(String idp, OIDCTokens oidcTokens) throws ParseException, IOException, java.text.ParseException {
        OIDCProviderMetadata oidcProviderMetadata = this.oidcProviders.get(idp).getMetadata();
        UserInfoRequest userInfoRequest = new UserInfoRequest(oidcProviderMetadata.getUserInfoEndpointURI(), oidcTokens.getAccessToken());
        UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoRequest.toHTTPRequest().send());

        if (!userInfoResponse.indicatesSuccess()) {
            UserInfoErrorResponse errorResponse = userInfoResponse.toErrorResponse();
            throw new OAuthException("UserInfo request failed: " + errorResponse.getErrorObject());
        }
        UserInfo userInfo = userInfoResponse.toSuccessResponse().getUserInfo();

        //if ansattporten, check for authorization_details claim
        if (IDPSelector.ANSATTPORTEN.equals(idp)) {
            List<AuthorizationDetail> authorizationDetailsClaim = getAuthorizationDetailsClaim(oidcTokens.getIDToken().getJWTClaimsSet());
            validateAuthorizationDetailsClaims(authorizationDetailsClaim);
            if (CollectionUtils.isNotEmpty(authorizationDetailsClaim)) {
                String eJusticeNaturalPersonRoleClaim = getEJusticeRoleClaim(authorizationDetailsClaim);
                if (eJusticeNaturalPersonRoleClaim != null) {
                    userInfo.setClaim(E_JUSTICE_NATURAL_PERSON_ROLE_CLAIM, eJusticeNaturalPersonRoleClaim);
                }
            }
        }
        return userInfo;
    }

    protected void validateAuthorizationDetailsClaims(List<AuthorizationDetail> authorizationDetailsClaim) {
        if (CollectionUtils.isEmpty(authorizationDetailsClaim)) {
            return;
        }

        var props = oidcProviders.get(IDPSelector.ANSATTPORTEN).getProperties().getAuthorizationDetails();
        if (CollectionUtils.isEmpty(props)) {
            log.warn("No configured authorization_details for Ansattporten; skipping validation");
            return; //should never happen(tm)
        }

        // Get allowed type:resource pairs from configuration
        Set<String> allowedTypeResourceList = oidcProviders.get(IDPSelector.ANSATTPORTEN)
                .getProperties().getConfiguredAuthDetailTypeResourceList();

        if (allowedTypeResourceList == null || allowedTypeResourceList.isEmpty()) {
            log.warn("No configured type:resource pairs derived for Ansattporten; skipping validation");
            return; // Misconfiguration protection; avoids NPE below
        }

        for (AuthorizationDetail claimAd : authorizationDetailsClaim) {
            String type = claimAd.getType() != null ? claimAd.getType().getValue() : null;
            String resource = claimAd.getStringField(RESOURCE);
            String key = (type != null && resource != null) ? (type + ":" + resource) : null;

            if (key == null || !allowedTypeResourceList.contains(key)) {
                throw new OAuthException(
                        "authorization_details claim contains unsupported type/resource: type=" + type +
                                ", resource=" + resource
                );
            }
        }
    }


    protected List<AuthorizationDetail> getAuthorizationDetailsClaim(JWTClaimsSet claims) {
        Object authDetailsClaim = claims.getClaim(AUTHORIZATION_DETAILS_CLAIM);
        if (authDetailsClaim == null) {
            return List.of();
        }

        // Normalize to a list of map-like JSON objects
        List<?> rawList;
        if (authDetailsClaim instanceof List<?> l) {
            rawList = l;
        } else if (authDetailsClaim instanceof Map<?, ?> m) {
            // Tolerate single object shape by wrapping into a list
            rawList = List.of(m);
        } else {
            log.warn("authorization_details claim must be a JSON object or list, but was: {}", authDetailsClaim.getClass());
            return List.of();
        }

        if (rawList.isEmpty()) {
            return List.of();
        }

        java.util.ArrayList<JSONObject> jsonList = new java.util.ArrayList<>(rawList.size());
        for (Object el : rawList) {
            if (el instanceof JSONObject jo) {
                jsonList.add(jo);
            } else if (el instanceof Map<?, ?> m) {
                JSONObject jo = new JSONObject();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() != null) {
                        jo.put(e.getKey().toString(), e.getValue());
                    }
                }
                jsonList.add(jo);
            } else {
                log.warn("authorization_details list contains non-JSONObject element: {}", el == null ? "null" : el.getClass());
                return List.of();
            }
        }

        try {
            return AuthorizationDetail.parseList(jsonList);
        } catch (ParseException e) {
            log.warn("Failed to parse authorization_details list: {}", jsonList, e);
            return List.of();
        }
    }

    //hardkodet, men lag gjerne konfig om det blir flere
    protected String getEJusticeRoleClaim(List<AuthorizationDetail> authorizationDetails) {
        if (authorizationDetails.stream().anyMatch(a -> URN_ALTINN_RESOURCE_BORIS_VIP_1_TILGANG.equals(a.getStringField(RESOURCE)))) {
            return "VIP1";
        }
        if (authorizationDetails.stream().anyMatch(a -> URN_ALTINN_RESOURCE_BORIS_VIP_2_TILGANG.equals(a.getStringField(RESOURCE)))) {
            return "VIP2";
        }
        return null;
    }

    protected ClientAuthentication clientAuthentication(String idp) {
        OIDCIntegrationProperties oidcIntegrationProperties = this.oidcProviders.get(idp).getProperties();
        ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.parse(oidcIntegrationProperties.getClientAuthMethod());
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(clientAuthenticationMethod)) {
            if (jwtGrantGenerator.isEmpty()) {
                throw new IllegalStateException("JWT Grant Generator is not present for PRIVATE_KEY_JWT authentication");
            }
            return jwtGrantGenerator.get().create(idp);
        } else if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(clientAuthenticationMethod)) {
            return new ClientSecretBasic(new ClientID(oidcIntegrationProperties.getClientId()), new Secret(oidcIntegrationProperties.getClientSecret()));
        } else if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(clientAuthenticationMethod)) {
            return new ClientSecretPost(new ClientID(oidcIntegrationProperties.getClientId()), new Secret(oidcIntegrationProperties.getClientSecret()));
        } else {
            throw new IllegalStateException(String.format("Unknown client authentication method %s. Valid methods are PRIVATE_KEY_JWT, CLIENT_SECRET_BASIC, and CLIENT_SECRET_POST.", clientAuthenticationMethod));
        }
    }


    public URI getAuthorizationEndpoint(String idp) {
        return this.oidcProviders.get(idp).getMetadata().getAuthorizationEndpointURI();
    }

    public String getIssuer(String idp) {
        return this.oidcProviders.get(idp).getMetadata().getIssuer().toString();
    }

}
