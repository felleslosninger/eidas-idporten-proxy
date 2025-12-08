package no.idporten.eidas.proxy.integration.idp.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.service.IDPSelector;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "eidas")
public class OIDCIntegrationPropertiesMap {
    @NotEmpty
    private Map<String, OIDCIntegrationProperties> oidcIntegrations;

    public OIDCIntegrationProperties get(String idp) {
        return oidcIntegrations.getOrDefault(idp, oidcIntegrations.get(IDPSelector.IDPORTEN));
    }

    @PostConstruct
    public void validate() {
        if (oidcIntegrations.isEmpty()) throw new IllegalArgumentException("No oidc integrations configured");
        if (!oidcIntegrations.containsKey(IDPSelector.IDPORTEN))
            throw new IllegalArgumentException("No oidc integration configured for idporten");
        if (!oidcIntegrations.containsKey(IDPSelector.ANSATTPORTEN))
            throw new IllegalArgumentException("No oidc integration configured for ansattporten");
    }
}
