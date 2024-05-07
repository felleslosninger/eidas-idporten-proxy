package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IDPCallback {

    private final SpecificProxyService specificProxyService;
    private final OIDCIntegrationService oidcIntegrationService;
    private final SpecificCommunicationService specificCommunicationService;



    @GetMapping("/idpcallback")
    public String callback(HttpServletRequest request) throws Exception {
        URI authorizationResponseUri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString()).query(request.getQueryString()).build().toUri();
        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(authorizationResponseUri);
        CorrelatedRequestHolder cachedRequest = specificProxyService.getCachedRequest(authorizationResponse.getState());
        if (cachedRequest == null) {
            throw new SpecificCommunicationException("No request found in eidas-idporten-proxy for state %s after idpcallback. ".formatted(authorizationResponse.getState()));
        }
        AuthorizationCode code = oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest);
        OIDCTokens tokens = oidcIntegrationService.getToken(code, cachedRequest.getAuthenticationRequest().getCodeVerifier(), cachedRequest.getAuthenticationRequest().getNonce());

        UserInfo userInfo = oidcIntegrationService.getUserInfo(tokens);
        //todo get acr from token
        LightResponse lightResponse = specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), LevelOfAssurance.EIDAS_LOA_LOW);
        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);

        return "redirect:%s?token=%s&relayState=%s".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64, lightResponse.getRelayState());
    }





}
