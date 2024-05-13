package no.idporten.eidas.proxy.service;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.config.EuProxyProperties;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import no.idporten.eidas.proxy.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static eu.eidas.auth.commons.light.ILevelOfAssurance.EIDAS_LOA_LOW;

/**
 * The main service
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(EuProxyProperties.class)
public class SpecificProxyService {

    private final SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    private final OIDCRequestCache oidcRequestCache;
    private final OIDCIntegrationService oidcIntegrationService;
    private final EuProxyProperties euProxyProperties;
    private static final String FAMILY_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName";
    private static final String FIRST_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName";
    private static final String DATE_OF_BIRTH = "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth";
    private static final String PID = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";
    public String getEuProxyRedirectUri() {
        return euProxyProperties.getRedirectUri();
    }

    public String createStoreBinaryLightTokenResponseBase64(ILightResponse lightResponse) throws SpecificCommunicationException {
        BinaryLightToken binaryLightToken = specificCommunicationServiceImpl.putResponse(lightResponse);
        return BinaryLightTokenHelper.encodeBinaryLightTokenBase64(binaryLightToken);
    }

    public AuthenticationRequest translateNodeRequest(ILightRequest originalIlightRequest) {
        CodeVerifier codeVerifier = new CodeVerifier();
        final AuthenticationRequest authenticationRequest = oidcIntegrationService.createAuthenticationRequest(codeVerifier);

        final CorrelatedRequestHolder correlatedRequestHolder = new CorrelatedRequestHolder(originalIlightRequest,
                new OIDCRequestStateParams(authenticationRequest.getState(),
                        authenticationRequest.getNonce(),
                        codeVerifier));
        oidcRequestCache.put(authenticationRequest.getState().getValue(), correlatedRequestHolder);

        return authenticationRequest;
    }

    public CorrelatedRequestHolder getCachedRequest(State state) {
        CorrelatedRequestHolder correlatedRequestHolder = oidcRequestCache.get(state.getValue());
        oidcRequestCache.remove(state.getValue());
        return correlatedRequestHolder;
    }

    public LightResponse getLightResponse(UserInfo userInfo, ILightRequest lightRequest, String acr) {
        LightResponse.LightResponseBuilder lightResponseBuilder = LightResponse.builder()
                .id(UUID.randomUUID().toString())
                .citizenCountryCode("NO")
                .consent("yes")
                .levelOfAssurance(getLoa(acr))
                .issuer(oidcIntegrationService.getIssuer())
                .subject(userInfo.getSubject().getValue())
                .subjectNameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .status(Status.builder().statusCode(EIDASStatusCode.SUCCESS_URI.getValue()).failure(false).statusMessage("ok").build())
                .inResponseToId(lightRequest.getId())
                .relayState(lightRequest.getRelayState());
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(FAMILY_NAME, List.of(userInfo.getFamilyName())));
        }
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(FIRST_NAME, List.of(userInfo.getGivenName())));
        }
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(DATE_OF_BIRTH, List.of(userInfo.getStringClaim("birth_date"))));
        }
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(PID, List.of(userInfo.getStringClaim("pid"))));
        }


        return lightResponseBuilder.build();
    }


    //hack sorry
    private static String getLoa(String acr) {
        try {
            String suffix = acr.split("-")[2];

            return switch (suffix) {
                case "high" -> LevelOfAssurance.EIDAS_LOA_HIGH;
                case "substantial" -> EIDAS_LOA_LOW;
                default -> EIDAS_LOA_LOW;
            };
        } catch (ArrayIndexOutOfBoundsException e) {
            return EIDAS_LOA_LOW;  // Return default LOA if the acr format is incorrect
        }
    }
}
