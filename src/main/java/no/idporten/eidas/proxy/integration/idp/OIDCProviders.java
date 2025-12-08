package no.idporten.eidas.proxy.integration.idp;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.idporten.eidas.proxy.service.IDPSelector;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

@Data
@AllArgsConstructor
public class OIDCProviders implements InitializingBean {
    @NotEmpty
    private Map<String, OIDCProvider> providers;

    public OIDCProvider get(String idp) {
        return providers.getOrDefault(idp, providers.get(IDPSelector.IDPORTEN));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (providers.isEmpty()) throw new IllegalArgumentException("No OIDC providers configured");
        if (!providers.containsKey(IDPSelector.IDPORTEN))
            throw new IllegalArgumentException("No OIDC provider configured for idporten");
        if (!providers.containsKey(IDPSelector.ANSATTPORTEN))
            throw new IllegalArgumentException("No OIDC provider configured for ansattporten");
    }
}
