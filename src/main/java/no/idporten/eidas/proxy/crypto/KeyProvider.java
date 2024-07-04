package no.idporten.eidas.proxy.crypto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Getter
public class KeyProvider {

    private final PrivateKey privateKey;
    private final java.security.cert.Certificate certificate;
    private final List<Certificate> certificateChain;

    public KeyProvider(KeyStore keyStore, String alias, String password) {
        try {
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
            certificateChain = Arrays.asList(keyStore.getCertificateChain(alias));
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            log.error("Failed to load keystore.", e);
            throw new IDPortenKeyStoreException("Failed to load keystore.", e);
        }
    }

}
