package no.idporten.eidas.proxy.config;

import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "eidas.eu-proxy")
public class EuProxyProperties {

    @NonNull
    private String redirectUri;

}
