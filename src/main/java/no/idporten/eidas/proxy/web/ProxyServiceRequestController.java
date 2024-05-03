/*
 * Copyright (c) 2023 by European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/page/eupl-text-11-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import eu.eidas.auth.commons.EidasParameterKeys;
import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.lightprotocol.IncomingLightRequestValidator;
import no.idporten.eidas.proxy.lightprotocol.LightRequest;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

/**
 * Receives/processes the servlet request that contains the token
 * used to retrieve the {@link ILightRequest} coming from the eIDAS-Node,
 * transforms it into an MS specific request and sends it to the receiver at the IdP.
 * <p>
 * If a user consent is needed forwards to a consent page instead.
 *
 * @since 2.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProxyServiceRequestController {


    private final SpecificCommunicationService specificCommunicationService;
    private final SpecificProxyService specificProxyService;
    private final EidasCacheProperties eidasCacheProperties;
    private final OIDCIntegrationService oidcIntegrationService;

    @RequestMapping(path = "/ProxyServiceRequest", method = {RequestMethod.GET, RequestMethod.POST})
    public String execute(@Nonnull final HttpServletRequest httpServletRequest) throws IOException, ServletException, ParseException {

        final ILightRequest lightRequest = getIncomingiLightRequest(httpServletRequest, null);
        if (IncomingLightRequestValidator.validateRequest((LightRequest) lightRequest)) {
            log.error("Incoming Light Request is invalid. Should not proceed, but we proceed for testing purposes.");
        }
        //skip consent flow for now
        final AuthenticationRequest authenticationRequest = createSpecificRequest(lightRequest);
        URI uri = oidcIntegrationService.pushedAuthorizationRequest(authenticationRequest);
        URI authUri = new AuthorizationRequest.Builder(uri, authenticationRequest.getClientID())
                .endpointURI(oidcIntegrationService.getAuthorizationEndpoint())
                .build()
                .toURI();
        return "redirect:%s".formatted(authUri.toString());
    }


    private ILightRequest getIncomingiLightRequest(@Nonnull HttpServletRequest httpServletRequest, final Collection<AttributeDefinition<?>> registry) throws ServletException {
        final String tokenBase64 = BinaryLightTokenHelper.getBinaryToken(httpServletRequest, EidasParameterKeys.TOKEN.toString());
        try {
            String lightTokenId = BinaryLightTokenHelper.getBinaryLightTokenId(tokenBase64, eidasCacheProperties.getRequestSecret(), eidasCacheProperties.getAlgorithm());
            return specificCommunicationService.getAndRemoveRequest(lightTokenId, registry);
        } catch (SpecificCommunicationException e) {
            log.error("Error unmarshalling MS Specific Request" + e);
            throw new ServletException(e);
        }
    }


    private AuthenticationRequest createSpecificRequest(ILightRequest originalIlightRequest) {
        return specificProxyService.translateNodeRequest(originalIlightRequest);
    }


}
