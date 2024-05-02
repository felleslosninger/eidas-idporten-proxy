package no.idporten.eidas.proxy.integration.specificcommunication.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@ConfigurationProperties(prefix = "eidas.cache")
@Validated
public class EidasCacheProperties {

    public static final String LIGHT_REQUEST = "light-request";
    public static final String LIGHT_RESPONSE = "light-response";
    public static final String IDP_REQUEST = "correlation-map";
    private static final String PREFIX_TEMPLATE = "%s:%s:%s";

    @NotEmpty
    private String name = "eidas-proxy";

    private String requestSecret = "secret";
    private String responseSecret = "secret";
    private String algorithm = "SHA256";
    private String responseIssuerName = "specificCommunicationDefinitionProxyserviceRequest";
    private String requestIssuerName = "specificCommunicationDefinitionProxyserviceResponse";
    public String getLightRequestPrefix(String id) {
        return PREFIX_TEMPLATE.formatted(name, LIGHT_REQUEST, id);
    }

    public String getLightResponsePrefix(String id) {
        return PREFIX_TEMPLATE.formatted(name, LIGHT_RESPONSE, id);
    }

    public String getIdpRequestPrefix(String id) {
        return PREFIX_TEMPLATE.formatted(name, IDP_REQUEST, id);
    }

}
