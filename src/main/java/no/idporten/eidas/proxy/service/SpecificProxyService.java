package no.idporten.eidas.proxy.service;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.config.EuProxyProperties;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import no.idporten.eidas.proxy.logging.MDCFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The main service
 */
@Service
@RequiredArgsConstructor
public class SpecificProxyService {

    private static final String BIRTH_DATE_CLAIM = "birth_date";
    private static final String PID_CLAIM = "pid";
    private static final String URN_OASIS_NAMES_TC_SAML_2_0_NAMEID_FORMAT_PERSISTENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
    private static final String NO_COUNTRY_CODE = "NO";
    private final SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    private final OIDCRequestCache oidcRequestCache;
    private final OIDCIntegrationService oidcIntegrationService;
    private final EuProxyProperties euProxyProperties;
    private final LevelOfAssuranceHelper levelOfAssuranceHelper;
    private static final String FAMILY_NAME_EIDAS = "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName";
    private static final String FIRST_NAME_EIDAS = "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName";
    private static final String DATE_OF_BIRTH_EIDAS = "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth";
    private static final String PID_EIDAS = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";

    public String getEuProxyRedirectUri() {
        return euProxyProperties.getRedirectUri();
    }

    public String createStoreBinaryLightTokenResponseBase64(ILightResponse lightResponse) throws SpecificProxyException {
        BinaryLightToken binaryLightToken = specificCommunicationServiceImpl.putResponse(lightResponse);
        return BinaryLightTokenHelper.encodeBinaryLightTokenBase64(binaryLightToken);
    }

    public AuthenticationRequest translateNodeRequest(ILightRequest originalIlightRequest) {
        CodeVerifier codeVerifier = new CodeVerifier();
        final AuthenticationRequest authenticationRequest = oidcIntegrationService.createAuthenticationRequest(
                codeVerifier,
                levelOfAssuranceHelper.eidasAcrListToIdportenAcrList(originalIlightRequest.getLevelsOfAssurance()),
                originalIlightRequest.getSpCountryCode());

        final CorrelatedRequestHolder correlatedRequestHolder = new CorrelatedRequestHolder(originalIlightRequest,
                new OIDCRequestStateParams(authenticationRequest.getState(),
                        authenticationRequest.getNonce(),
                        codeVerifier,
                        MDCFilter.getTraceId()));
        oidcRequestCache.put(authenticationRequest.getState().getValue(), correlatedRequestHolder);

        return authenticationRequest;
    }



    public CorrelatedRequestHolder getCachedRequest(State state) {
        CorrelatedRequestHolder correlatedRequestHolder = oidcRequestCache.get(state.getValue());
        oidcRequestCache.remove(state.getValue());
        return correlatedRequestHolder;
    }

    public LightResponse getLightResponse(UserInfo userInfo, ILightRequest lightRequest, ILevelOfAssurance acr) {

        LightResponse.LightResponseBuilder lightResponseBuilder = LightResponse.builder()
                .id(UUID.randomUUID().toString())
                .citizenCountryCode(NO_COUNTRY_CODE)
                .consent("yes")
                .levelOfAssurance(acr.getValue())
                .issuer(oidcIntegrationService.getIssuer())
                .subject(userInfo.getSubject().getValue())
                .subjectNameIdFormat(URN_OASIS_NAMES_TC_SAML_2_0_NAMEID_FORMAT_PERSISTENT)
                .status(Status.builder().statusCode(EIDASStatusCode.SUCCESS_URI.getValue()).failure(false).statusMessage("ok").build())
                .inResponseToId(lightRequest.getId())
                .relayState(lightRequest.getRelayState());
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(FAMILY_NAME_EIDAS, List.of(userInfo.getFamilyName())));
        }
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(FIRST_NAME_EIDAS, List.of(userInfo.getGivenName())));
        }
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(DATE_OF_BIRTH_EIDAS, List.of(userInfo.getStringClaim(BIRTH_DATE_CLAIM))));
        }
        if (StringUtils.isNotEmpty(userInfo.getFamilyName())) {
            lightResponseBuilder.attribute(new Attribute(PID_EIDAS, List.of(userInfo.getStringClaim(PID_CLAIM))));
        }


        return lightResponseBuilder.build();
    }

    public LightResponse getErrorLightResponse(EIDASStatusCode eidasStatusCode, Exception ex) {
        if (ex instanceof SpecificProxyException spex) {
            return LightResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .citizenCountryCode(NO_COUNTRY_CODE)
                    .issuer(oidcIntegrationService.getIssuer())
                    .inResponseToId(getInResponseToId(spex))
                    .relayState(getRelayState(spex))
                    .status(getErrorStatus(eidasStatusCode, spex.getMessage()))
                    .build();
        } else {
            return LightResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .citizenCountryCode(NO_COUNTRY_CODE)
                    .issuer(oidcIntegrationService.getIssuer())
                    .status(getErrorStatus(eidasStatusCode, "An internal error occurred"))
                    .build();
        }

    }

    private static String getInResponseToId(SpecificProxyException spex) {
        return spex.getLightRequest() != null ? spex.getLightRequest().getId() : null;
    }

    private static String getRelayState(SpecificProxyException ex) {
        return ex.getLightRequest() != null ? ex.getLightRequest().getRelayState() : null;
    }

    private static Status getErrorStatus(EIDASStatusCode eidasStatusCode, String message) {
        return Status.builder().statusCode(eidasStatusCode.getValue())
                .failure(true)
                .statusMessage(message).build();
    }


}
