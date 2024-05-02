package no.idporten.eidas.proxy.integration.idp.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "eidas.oidc-integration")
public class OIDCIntegrationProperties implements InitializingBean {

    @NotNull
    private URI issuer;

    @NotNull
    private URI redirectUri;

    @NotEmpty
    @Pattern(regexp = "^(client_secret_basic|client_secret_post|private_key_jwt)$")
    private String clientAuthMethod;

    @NotEmpty
    private String clientId;

    private String clientSecret;
    private String clientKeystoreType;
    private String clientKeystoreLocation;
    private String clientKeystorePassword;
    private String clientKeystoreKeyAlias;
    private String clientKeystoreKeyPassword;

    @Min(1)
    private int connectTimeOutMillis = 5000;
    @Min(1)
    private int readTimeOutMillis = 5000;
    @Min(1)
    private int jwksCacheRefreshMinutes = 5;
    @Min(1)
    private int jwksCacheLifetimeMinutes = 60;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (clientAuthMethod.startsWith("client_secret") && !StringUtils.hasText(clientSecret)) {
            notEmptyForClientAuth("client-secret", clientSecret, clientAuthMethod);
        }
        if (clientAuthMethod.equals("private_key_jwt")) {
            notEmptyForClientAuth("client-keystore-type", clientKeystoreType, clientAuthMethod);
            notEmptyForClientAuth("client-keystore-location", clientKeystoreLocation, clientAuthMethod);
            notEmptyForClientAuth("client-keystore-password", clientKeystorePassword, clientAuthMethod);
            notEmptyForClientAuth("client-keystore-key-alias", clientKeystoreKeyAlias, clientAuthMethod);
            notEmptyForClientAuth("client-keystore-key-password", clientKeystoreKeyPassword, clientAuthMethod);
        }

    }

    protected void notEmptyForClientAuth(String property, String value, String clientAuthMethod) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(String.format("Property %s must have a value when using client auth method %s", property, clientAuthMethod));
        }
    }


}
