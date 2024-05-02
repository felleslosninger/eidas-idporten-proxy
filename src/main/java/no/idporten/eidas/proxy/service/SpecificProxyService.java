package no.idporten.eidas.proxy.service;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

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


}
