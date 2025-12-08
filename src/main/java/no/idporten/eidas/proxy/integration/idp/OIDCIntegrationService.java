package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.jwt.ClientAssertionGenerator;
import no.idporten.eidas.proxy.logging.AuditService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

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
    private final OIDCProviders oidcProviders;
    private final Optional<ClientAssertionGenerator> jwtGrantGenerator;
    private final AuditService auditService;

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

        return builder.build();

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

    public UserInfo getUserInfo(String idp, OIDCTokens oidcTokens) throws ParseException, IOException {
        OIDCProviderMetadata oidcProviderMetadata = this.oidcProviders.get(idp).getMetadata();
        UserInfoRequest userInfoRequest = new UserInfoRequest(oidcProviderMetadata.getUserInfoEndpointURI(), oidcTokens.getAccessToken());
        UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoRequest.toHTTPRequest().send());

        if (!userInfoResponse.indicatesSuccess()) {
            UserInfoErrorResponse errorResponse = userInfoResponse.toErrorResponse();
            throw new OAuthException("UserInfo request failed: " + errorResponse.getErrorObject());
        }
        return userInfoResponse.toSuccessResponse().getUserInfo();
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
