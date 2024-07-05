package no.idporten.eidas.proxy.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "eidas.oidc-integration", name = "client-auth-method", havingValue = "private_key_jwt")
@EnableConfigurationProperties(KeystoreProperties.class)
public class CryptoConfiguration {

    @Bean
    public KeyStoreResourceLoader keyStoreResourceLoader() {
        return new KeyStoreResourceLoader();
    }

    @Bean
    public KeyStoreProvider keyStoreProvider(KeystoreProperties keystoreProperties, KeyStoreResourceLoader keyStoreResourceLoader) {
        return new KeyStoreProvider(
                keystoreProperties.getKeystoreType(),
                keystoreProperties.getKeystoreLocation(),
                keystoreProperties.getKeystorePassword(),
                keyStoreResourceLoader);
    }

    @Bean
    public KeyProvider keyProviderFromKeystore(KeyStoreProvider keyStoreProvider, KeystoreProperties keystoreProperties) {
        return new KeyProvider(
                keyStoreProvider.getKeyStore(),
                keystoreProperties.getKeyAlias(),
                keystoreProperties.getKeyPassword());
    }


}
