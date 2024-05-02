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

import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.attribute.PersonType;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.impl.LightRequest;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
public class FakeEUProxyServiceRequestController {


    private final SpecificCommunicationService specificCommunicationService;

    @GetMapping(path = "/fakeit")
    public String execute() throws SpecificCommunicationException {

        BinaryLightToken binaryLightToken = specificCommunicationService.putRequest(new LightRequest.Builder()
                .id("123")
                .requesterId("requesterId")
                .levelOfAssurance("wtf:eidas-loa-high")
                .issuer("CA")
                .providerName("providerName")
                .citizenCountryCode("NO")
                .requestedAttributes(ImmutableAttributeMap.builder()
                        .put(AttributeDefinition.builder()
                                .personType(PersonType.NATURAL_PERSON)
                                .xmlType("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName", "Local", "prefix")
                                .friendlyName("attribute1").nameUri("http://hey")
                                .attributeValueMarshaller("no.idporten.eidas.proxy.web.DummyAttributeValueMarshaller")
                                .build())
                        .build())
                .build());
        final String binaryLightTokenBase64 = BinaryLightTokenHelper.encodeBinaryLightTokenBase64(binaryLightToken);
        return "redirect:/ProxyServiceRequest?token=" + binaryLightTokenBase64;

    }


}
