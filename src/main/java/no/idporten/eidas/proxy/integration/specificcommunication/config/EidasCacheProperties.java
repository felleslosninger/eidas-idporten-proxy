package no.idporten.eidas.proxy.integration.specificcommunication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@ConfigurationProperties(prefix = "eidas.cache")
@Validated
public class EidasCacheProperties {

    public static final String LIGHT_REQUEST = "nodeSpecificProxyserviceRequestCache";
    public static final String LIGHT_RESPONSE = "specificNodeProxyserviceResponseCache";
    public static final String IDP_REQUEST = "correlation-map";
    private static final String PREFIX_TEMPLATE = "%s:%s";


    private String requestSecret;
    private String responseSecret;
    private String algorithm = "SHA256";
    private String responseIssuerName = "specificCommunicationDefinitionProxyserviceRequest";
    private String requestIssuerName = "specificCommunicationDefinitionProxyserviceResponse";
    public String getLightRequestPrefix(String id) {
        return PREFIX_TEMPLATE.formatted(LIGHT_REQUEST, id);
    }

    public String getLightResponsePrefix(String id) {
        return PREFIX_TEMPLATE.formatted(LIGHT_RESPONSE, id);
    }

    public String getIdpRequestPrefix(String id) {
        return PREFIX_TEMPLATE.formatted(IDP_REQUEST, id);
    }

}
