package no.idporten.eidas.proxy.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@NoArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "eidas.eu-proxy")
public class EuProxyProperties implements InitializingBean {

    @NonNull
    private String redirectUri;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Loaded EU Proxy properties: redirectUri {} ", this.redirectUri);
    }
}
