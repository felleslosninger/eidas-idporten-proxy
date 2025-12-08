package no.idporten.eidas.proxy.crypto;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
@Slf4j
public class KeystoreProperties {

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


    @PostConstruct
    public void afterPropertiesSet() {
        log.info("Loaded keystore properties: keystoreType {} keystore ...{} keyAlias {}",
                this.keystoreType,
                this.keystoreLocation.substring(this.keystoreLocation.length() - 6),
                this.keyAlias);
    }
}
