
package no.idporten.eidas.proxy.crypto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Slf4j
@Getter
public class KeyStoreProvider {

    private final KeyStore keyStore;

    public KeyStoreProvider(String type, String location, String password, KeyStoreResourceLoader resourceLoader) {
        try (InputStream is = resourceLoader.getResource(location).getInputStream()) {
            KeyStore keystore = KeyStore.getInstance(type);
            keystore.load(is, password.toCharArray());

            log.info("Loaded keystore of type {} from {}",
                    type,
                    location.startsWith("base64:")
                            ? String.format("%100.100s...", location)
                            : location);

            this.keyStore = keystore;
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            log.error("Failed to load keystoreprovider.", e);
            throw new IDPortenKeyStoreException("Failed to load keystore.", e);
        }
    }


}
