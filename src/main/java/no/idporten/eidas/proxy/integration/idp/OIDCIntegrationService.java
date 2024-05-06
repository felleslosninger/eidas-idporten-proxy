package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.crypto.KeyProvider;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OIDCIntegrationService {
    private final Optional<KeyProvider> keyProvider;
    private final IDTokenValidator idTokenValidator;
    private final OIDCIntegrationProperties oidcIntegrationProperties;
    private final OIDCProviderMetadata oidcProviderMetadata;
    final Scope scope = new Scope("openid", "eidas:mds");

    private final List<ACR> acrValues = List.of(new ACR("idporten-loa-substantial"));

    public AuthenticationRequest createAuthenticationRequest(CodeVerifier codeVerifier) {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(ResponseType.CODE,
                scope,
                new ClientID(oidcIntegrationProperties.getClientId()),
                oidcIntegrationProperties.getRedirectUri());
        builder.endpointURI(oidcProviderMetadata.getPushedAuthorizationRequestEndpointURI())
                .acrValues(acrValues)
                .state(new com.nimbusds.oauth2.sdk.id.State())
                .nonce(new Nonce())
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
        ;

        return builder.build();

    }

    public URI pushedAuthorizationRequest(AuthenticationRequest authenticationRequest) throws IOException, ParseException {
        final ClientAuthentication clientAuth = new ClientSecretBasic(new ClientID(oidcIntegrationProperties.getClientId()), new Secret(oidcIntegrationProperties.getClientSecret()));
        HTTPRequest httpRequest = new PushedAuthorizationRequest(
                oidcProviderMetadata.getPushedAuthorizationRequestEndpointURI(), clientAuth, authenticationRequest)
                .toHTTPRequest();
        HTTPResponse httpResponse = httpRequest.send();


        PushedAuthorizationResponse response = PushedAuthorizationResponse.parse(httpResponse);

        if (!response.indicatesSuccess()) {
            log.warn("PAR request failed: " + response.toErrorResponse().getErrorObject().getHTTPStatusCode());
            log.warn("Optional error code: " + response.toErrorResponse().getErrorObject().getCode());
            throw new OAuthException("PAR request failed: " + response.toErrorResponse().getErrorObject().getDescription());
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
            log.error("Authorization response indicates failure");
            AuthorizationErrorResponse errorResponse = AuthorizationErrorResponse.parse(authorizationResponse.toErrorResponse().toURI());
            throw new OAuthException("Authorization response indicates failure %s".formatted(errorResponse.getErrorObject().getDescription()));
        }

        AuthorizationSuccessResponse successResponse = authorizationResponse.toSuccessResponse();
        return successResponse.getAuthorizationCode();
    }

    public OIDCTokens getToken(AuthorizationCode code, CodeVerifier codeVerifier, Nonce nonce) throws IOException, ParseException, BadJOSEException, JOSEException {
        URI callback = oidcIntegrationProperties.getRedirectUri();
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback, codeVerifier);

        final ClientAuthentication clientAuth = new ClientSecretBasic(new ClientID(oidcIntegrationProperties.getClientId()), new Secret(oidcIntegrationProperties.getClientSecret()));

        TokenRequest request = new TokenRequest(oidcProviderMetadata.getTokenEndpointURI(), clientAuth, codeGrant);

        TokenResponse response = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse errorResponse = response.toErrorResponse();
            throw new OAuthException("Token request failed: " + errorResponse.getErrorObject());
        }
        OIDCTokenResponse successResponse = (OIDCTokenResponse) response.toSuccessResponse();
        IDTokenClaimsSet validate = idTokenValidator.validate(successResponse.getOIDCTokens().getIDToken(), nonce);
        return successResponse.getOIDCTokens();
    }

    public UserInfo getUserInfo(OIDCTokens oidcTokens) throws ParseException, IOException {


        UserInfoRequest userInfoRequest = new UserInfoRequest(oidcProviderMetadata.getUserInfoEndpointURI(), oidcTokens.getAccessToken());
        UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoRequest.toHTTPRequest().send());

        if (!userInfoResponse.indicatesSuccess()) {
            UserInfoErrorResponse errorResponse = userInfoResponse.toErrorResponse();
            throw new OAuthException("UserInfo request failed: " + errorResponse.getErrorObject());
        }
        return userInfoResponse.toSuccessResponse().getUserInfo();
    }

    protected ClientAuthentication clientAuthentication(OIDCIntegrationProperties oidcIntegrationProperties) {
        ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.parse(oidcIntegrationProperties.getClientAuthMethod());
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT == clientAuthenticationMethod) {
            return clientAssertion(oidcIntegrationProperties, keyProvider.get());
        }
        throw new IllegalStateException(String.format("Unknown client authentication method %s", clientAuthenticationMethod));
    }

    protected ClientAuthentication clientAssertion(OIDCIntegrationProperties oidcIntegrationProperties, KeyProvider keyProvider) {
        try {
            List<Base64> encodedCertificates = new ArrayList<>();
            for (Certificate c : keyProvider.certificateChain()) {
                encodedCertificates.add(Base64.encode(c.getEncoded()));
            }
            JWSHeader header = new JWSHeader
                    .Builder(JWSAlgorithm.RS256)
                    .x509CertChain(encodedCertificates)
                    .build();
            long created = Clock.systemUTC().millis();
            long expires = created + (120 * 1000L);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(oidcIntegrationProperties.getClientId())
                    .subject(oidcIntegrationProperties.getClientId())
                    .audience(oidcIntegrationProperties.getIssuer().toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(new Date(created))
                    .expirationTime(new Date(expires))
                    .build();
            JWSSigner signer = new RSASSASigner(keyProvider.privateKey());
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);
            return new PrivateKeyJWT(signedJWT);
        } catch (Exception e) {
            throw new OAuthException("Failed to create client assertion " + e.getMessage());
        }
    }

    public URI getAuthorizationEndpoint() {
        return oidcProviderMetadata.getAuthorizationEndpointURI();
    }

    public String getIssuer() {
        return oidcProviderMetadata.getIssuer().toString();
    }

}
