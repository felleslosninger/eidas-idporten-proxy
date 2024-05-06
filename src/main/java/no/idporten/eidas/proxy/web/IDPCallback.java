package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
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

import static eu.eidas.auth.commons.light.ILevelOfAssurance.EIDAS_LOA_PREFIX;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IDPCallback {

    private final SpecificProxyService specificProxyService;
    private final OIDCIntegrationService oidcIntegrationService;
    private final SpecificCommunicationService specificCommunicationService;

    private static final String FAMILY_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName";
    private static final String FIRST_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentFirstName";
    private static final String DATE_OF_BIRTH = "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth";
    private static final String PID = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";


    @GetMapping("/idpcallback")
    public String callback(HttpServletRequest request) throws Exception {
        URI authorizationResponseUri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString()).query(request.getQueryString()).build().toUri();
        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(authorizationResponseUri);
        CorrelatedRequestHolder cachedRequest = specificProxyService.getCachedRequest(authorizationResponse.getState());
        AuthorizationCode code = oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest);
        UserInfo userInfo = oidcIntegrationService.getUserInfo(code, cachedRequest.getAuthenticationRequest().getCodeVerifier(), cachedRequest.getAuthenticationRequest().getNonce());
        log.info("Got userinfo {}", userInfo.toJSONObject().toJSONString());
        LightResponse lightResponse = getLightResponse(userInfo, cachedRequest.getiLightRequest());
        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);

        return "redirect:%s?token=%s&relayState=".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64, lightResponse.getRelayState());
    }

    protected LightResponse getLightResponse(UserInfo userInfo, ILightRequest lightRequest) {

        return LightResponse.builder()
                .id(UUID.randomUUID().toString())
                .citizenCountryCode("NO")
                .consent("http://eidas.europa.eu/LoA/low")
                .levelOfAssurance(new LevelOfAssurance(EIDAS_LOA_PREFIX, LevelOfAssurance.EIDAS_LOA_HIGH))
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


}
