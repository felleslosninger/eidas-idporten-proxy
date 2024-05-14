package no.idporten.eidas.proxy.web;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.exceptions.ErrorCodes;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.ParseException;
import java.util.List;

import static eu.eidas.auth.commons.EidasParameterKeys.RELAY_STATE;


@Controller
@Slf4j
@RequiredArgsConstructor
public class IDPCallback {

    public static final String ACR_CLAIM = "acr";
    private final SpecificProxyService specificProxyService;
    private final OIDCIntegrationService oidcIntegrationService;
    private final SpecificCommunicationService specificCommunicationService;



    @GetMapping("/idpcallback")
    public String callback(HttpServletRequest request, HttpSession httpSession) throws Exception {
        URI authorizationResponseUri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString()).query(request.getQueryString()).build().toUri();
        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(authorizationResponseUri);
        CorrelatedRequestHolder cachedRequest = specificProxyService.getCachedRequest(authorizationResponse.getState());

        if (cachedRequest == null || cachedRequest.getiLightRequest() == null) {
            throw new SpecificProxyException(ErrorCodes.INVALID_SESSION.getValue(), "No request found in eidas-idporten-proxy for state %s after idpcallback. ".formatted(authorizationResponse.getState()), null);
        }
        httpSession.setAttribute(RELAY_STATE.getValue(), cachedRequest.getiLightRequest().getRelayState());

        AuthorizationCode code = oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest);
        OIDCTokens tokens = oidcIntegrationService.getToken(code, cachedRequest.getAuthenticationRequest().getCodeVerifier(), cachedRequest.getAuthenticationRequest().getNonce());

        UserInfo userInfo = oidcIntegrationService.getUserInfo(tokens);
        ILevelOfAssurance acrClaim = getAcrClaim(tokens.getIDToken().getJWTClaimsSet(), cachedRequest);
        LightResponse lightResponse = specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), acrClaim);
        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);

        return "redirect:%s?token=%s&relayState=%s".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64, lightResponse.getRelayState());
    }

    private ILevelOfAssurance getAcrClaim(JWTClaimsSet claimsSet, CorrelatedRequestHolder cachedRequest) throws ParseException, SpecificProxyException {
        String acrClaim = claimsSet.getStringClaim(ACR_CLAIM);
        ILevelOfAssurance received;
        if (!StringUtils.hasText(acrClaim)) {
            throw new SpecificProxyException(ErrorCodes.INTERNAL_ERROR.getValue(), "No acr claim found in id_token %s".formatted(claimsSet), cachedRequest.getiLightRequest().getRelayState());
        } else {
            received = specificProxyService.idportenAcrListToEidasAcr(acrClaim);
            if (!isLoaValid(received, cachedRequest.getiLightRequest().getLevelsOfAssurance())) {
                throw new SpecificProxyException(ErrorCodes.INVALID_SESSION.getValue(), "Invalid LoA %s for user %s. Requested was %s".formatted(acrClaim, claimsSet, cachedRequest.getiLightRequest().getLevelsOfAssurance()), cachedRequest.getiLightRequest().getRelayState());
            }
        }
        return received;
    }


    public boolean isLoaValid(ILevelOfAssurance received, List<ILevelOfAssurance> requested) {
        if (CollectionUtils.isEmpty(requested)) {
            return true;
        }
        return requested.contains(received);
    }

}
