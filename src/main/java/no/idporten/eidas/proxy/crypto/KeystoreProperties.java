package no.idporten.eidas.proxy.crypto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
@Slf4j
@ConfigurationProperties(ignoreInvalidFields = true, prefix = "eidas.oidc-integration.keystore")
public class KeystoreProperties implements InitializingBean {

    @NotNull
    private String keystoreType;
    @NotNull
    private String keystoreLocation;
    @NotNull
    private String keystorePassword;
    @NotNull
    private String keyAlias;
    @NotNull
    private String keyPassword;


    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Loaded keystore properties: keystoreType {} keystore ...{} keyAlias {}",
                this.keystoreType,
                this.keystoreLocation.substring(this.keystoreLocation.length() - 6),
                this.keyAlias);
    }
}
