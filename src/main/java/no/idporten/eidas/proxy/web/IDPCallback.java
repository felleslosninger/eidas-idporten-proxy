package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.attribute.AttributeValue;
import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.attribute.PersonType;
import eu.eidas.auth.commons.attribute.impl.StringAttributeValue;
import eu.eidas.auth.commons.attribute.impl.StringAttributeValueMarshaller;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.impl.LightResponse;
import eu.eidas.auth.commons.light.impl.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.idp.OIDCIntegrationService;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.proxy.service.SpecificProxyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.UUID;

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
        AuthorizationCode code = oidcIntegrationService.getAuthorizationCode(authorizationResponse, cachedRequest);
        UserInfo userInfo = oidcIntegrationService.getUserInfo(code, cachedRequest.getAuthenticationRequest().getCodeVerifier(), cachedRequest.getAuthenticationRequest().getNonce());

        LightResponse lightResponse = getLightResponse(userInfo, cachedRequest.getiLightRequest());

        String storeBinaryLightTokenResponseBase64 = specificProxyService.createStoreBinaryLightTokenResponseBase64(lightResponse);
        specificCommunicationService.putResponse(lightResponse);

        return "redirect:%s?token=%s&relayState=".formatted(specificProxyService.getEuProxyRedirectUri(), storeBinaryLightTokenResponseBase64, lightResponse.getRelayState());
    }

    protected LightResponse getLightResponse(UserInfo userInfo, ILightRequest lightRequest) {
        // Assuming 'FAMILY_NAME' is the AttributeDefinition for the family name
        // Example of fetching a predefined AttributeDefinition for Family Name
        //todo add all claims friendly name, birthdate and over 18 claim
        AttributeDefinition<String> familyNameDef = new AttributeDefinition.Builder<String>()
                .nameUri(URI.create("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName"))
                .friendlyName(userInfo.getGivenName())
                .personType(PersonType.NATURAL_PERSON)
                .required(true)
                .transliterationMandatory(false)
                .uniqueIdentifier(false)
                .xmlType(new QName("http://eidas.europa.eu/attributes/naturalperson", "CurrentFamilyName", "eidas"))
                .attributeValueMarshaller(new StringAttributeValueMarshaller())  // Assuming you have a specific marshaller for string attributes
                .build();

        // Extract the family name value from the ID token claims set
        String familyNameValue = userInfo.getFamilyName();

        // Create an AttributeValue for the family name
        AttributeValue<String> familyNameAttr = new StringAttributeValue(familyNameValue);

        return LightResponse.builder()
                .id(UUID.randomUUID().toString())
                .issuer(oidcIntegrationService.getIssuer())
                .subject(userInfo.getSubject().getValue())
                .attributes(ImmutableAttributeMap.builder()
                        .put(familyNameDef, familyNameAttr)
                        .build())
                .subjectNameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .status(ResponseStatus.builder().statusCode("200").build())
                .inResponseToId(lightRequest.getId())
                .relayState(lightRequest.getRelayState()).build();
    }


}
