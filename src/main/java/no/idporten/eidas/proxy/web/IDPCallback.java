package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static eu.eidas.auth.commons.light.ILevelOfAssurance.EIDAS_LOA_LOW;
import static eu.eidas.auth.commons.light.ILevelOfAssurance.EIDAS_LOA_PREFIX;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IDPCallback {

    private final SpecificProxyService specificProxyService;
    private final OIDCIntegrationService oidcIntegrationService;
    private final SpecificCommunicationService specificCommunicationService;

    private static final String FAMILY_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName";
    private static final String FIRST_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName";
    private static final String DATE_OF_BIRTH = "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth";
    private static final String PID = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";


    @GetMapping("/idpcallback")
    public String callback(HttpServletRequest request) throws Exception {
        URI authorizationResponseUri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString()).query(request.getQueryString()).build().toUri();
        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(authorizationResponseUri);
        CorrelatedRequestHolder cachedRequest = specificProxyService.getCachedRequest(authorizationResponse.getState());
        AuthorizationCode code = oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest);
        OIDCTokens tokens = oidcIntegrationService.getToken(code, cachedRequest.getAuthenticationRequest().getCodeVerifier(), cachedRequest.getAuthenticationRequest().getNonce());

        UserInfo userInfo = oidcIntegrationService.getUserInfo(tokens);
        log.info("Got userinfo {}", userInfo.toJSONObject().toJSONString());
        LightResponse lightResponse = getLightResponse(userInfo, cachedRequest.getiLightRequest(), EIDAS_LOA_PREFIX + "SUBSTANTIAL");
        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);

        return "redirect:%s?token=%s&relayState=".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64, lightResponse.getRelayState());
    }

    protected LightResponse getLightResponse(UserInfo userInfo, ILightRequest lightRequest, String acr) {
        return LightResponse.builder()
                .id(UUID.randomUUID().toString())
                .citizenCountryCode("NO")
                .consent("yes")
                .levelOfAssurance(getLoa(acr))
                .issuer(oidcIntegrationService.getIssuer())
                .subject(userInfo.getSubject().getValue())
                .attribute(new Attribute(FAMILY_NAME, List.of(userInfo.getFamilyName())))
                .attribute(new Attribute(FIRST_NAME, List.of(userInfo.getGivenName())))
                .attribute(new Attribute(DATE_OF_BIRTH, List.of(userInfo.getStringClaim("birth_date"))))
                .attribute(new Attribute(PID, List.of(userInfo.getStringClaim("pid"))))
                .subjectNameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .status(Status.builder().statusCode(EIDASStatusCode.SUCCESS_URI.getValue()).failure(false).statusMessage("ok").build())
                .inResponseToId(lightRequest.getId())
                .relayState(lightRequest.getRelayState()).build();
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
