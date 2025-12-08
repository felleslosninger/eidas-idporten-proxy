package no.idporten.eidas.proxy.integration.idp.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.crypto.KeystoreProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Set;

@Data
@Slf4j
@Validated
public class OIDCIntegrationProperties {

    @NotNull
    private URI issuer;

    @NotNull
    private URI redirectUri;

    @NotEmpty
    @Pattern(regexp = "^(client_secret_basic|client_secret_post|private_key_jwt)$")
    private String clientAuthMethod;

    @NotEmpty
    private String clientId;

    @NotEmpty
    private Set<String> scopes;

    private String clientSecret;
    private String keyId;

    @Min(1)
    private int connectTimeoutMillis = 5000;
    @Min(1)
    private int readTimeoutMillis = 5000;
    @Min(1)
    private int jwksCacheRefreshMinutes = 5;
    @Min(1)
    private int jwksCacheLifetimeMinutes = 60;

    private KeystoreProperties keystore;

    @PostConstruct
    public void afterPropertiesSet() {
        if (clientAuthMethod.startsWith("client_secret") && !StringUtils.hasText(clientSecret)) {
            notEmptyForClientAuth("client-secret", clientSecret, clientAuthMethod);
        }
    }

    protected void notEmptyForClientAuth(String property, String value, String clientAuthMethod) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(String.format("Property %s must have a value when using client auth method %s", property, clientAuthMethod));
        }
        if (StringUtils.hasText(keyId)) {
            log.info("KeyId (kid) set to {}", keyId);
        }
    }


}
