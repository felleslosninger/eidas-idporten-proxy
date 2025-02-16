package no.idporten.eidas.proxy.web;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.exceptions.ErrorCodes;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.logging.AuditService;
import no.idporten.eidas.proxy.service.LevelOfAssuranceHelper;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import no.idporten.sdk.oidcserver.protocol.FormPostResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.ParseException;
import java.util.Map;


@Controller
@Slf4j
@RequiredArgsConstructor
public class IDPCallback {

    public static final String ACR_CLAIM = "acr";
    private final SpecificProxyService specificProxyService;
    private final OIDCIntegrationService oidcIntegrationService;
    private final SpecificCommunicationService specificCommunicationService;
    private final LevelOfAssuranceHelper levelOfAssuranceHelper;
    private final AuditService auditService;


    @GetMapping("/idpcallback")
    public void callback(HttpServletRequest request,
                         HttpServletResponse response
    ) throws Exception {
        URI authorizationResponseUri = UriComponentsBuilder.fromUriString(request.getRequestURL().toString()).query(request.getQueryString()).build().toUri();
        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(authorizationResponseUri);
        CorrelatedRequestHolder cachedRequest = specificProxyService.getCachedRequest(authorizationResponse.getState());

        if (cachedRequest == null || cachedRequest.getiLightRequest() == null) {
            throw new SpecificProxyException(ErrorCodes.INVALID_SESSION.getValue(), "No request found in eidas-idporten-proxy for state %s after idpcallback. ".formatted(authorizationResponse.getState()), null);
        }

        try {
            AuthorizationCode code = oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest);
            OIDCTokens tokens = oidcIntegrationService.getToken(code, cachedRequest.getAuthenticationRequest().getCodeVerifier(), cachedRequest.getAuthenticationRequest().getNonce());

            UserInfo userInfo = oidcIntegrationService.getUserInfo(tokens);

            ILevelOfAssurance acrClaim = getAcrClaim(tokens.getIDToken().getJWTClaimsSet(), cachedRequest);
            LightResponse lightResponse = specificProxyService.getLightResponse(userInfo, cachedRequest.getiLightRequest(), acrClaim);
            auditService.auditLightResponse(lightResponse, cachedRequest.getAuthenticationRequest().getRequestTraceId());
            String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
            FormPostResponse formPostResponse = new FormPostResponse(specificProxyService.getEuProxyRedirectUri(),
                    Map.of("token", storeBinaryLightTokenResponseBase64));
            response.setContentType("text/html;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(formPostResponse.getRedirectForm());
        } catch (OAuthException e) {
            throw new SpecificProxyException(ErrorCodes.INTERNAL_ERROR.getValue(), "Error getting tokens from OIDC provider: %s".formatted(e.getMessage()), cachedRequest.getiLightRequest());
        }
    }

    private ILevelOfAssurance getAcrClaim(JWTClaimsSet claimsSet, CorrelatedRequestHolder cachedRequest) throws ParseException, SpecificProxyException {
        String acrClaim = claimsSet.getStringClaim(ACR_CLAIM);
        ILevelOfAssurance received;
        if (!StringUtils.hasText(acrClaim)) {
            throw new SpecificProxyException(ErrorCodes.INTERNAL_ERROR.getValue(), "No acr claim found in id_token %s".formatted(claimsSet), cachedRequest.getiLightRequest());
        } else {
            received = levelOfAssuranceHelper.idportenAcrListToEidasAcr(acrClaim);
            if (!levelOfAssuranceHelper.hasValidAcrLevel(received.getValue(), cachedRequest.getiLightRequest().getLevelsOfAssurance().stream().map(ILevelOfAssurance::getValue).toList())) {
                throw new SpecificProxyException(ErrorCodes.INVALID_SESSION.getValue(), "Invalid LoA %s for user %s. Requested was %s".formatted(acrClaim, claimsSet, cachedRequest.getiLightRequest().getLevelsOfAssurance()), cachedRequest.getiLightRequest());
            }
        }
        return received;
    }


}
