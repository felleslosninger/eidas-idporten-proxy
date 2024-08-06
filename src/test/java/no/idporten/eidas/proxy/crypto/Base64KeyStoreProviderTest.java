package no.idporten.eidas.proxy.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("When loading keystores")
@SpringBootTest
@ActiveProfiles("base64")
class Base64KeyStoreProviderTest {

    @Autowired
    private KeyStoreProvider keyStoreProvider;

    @Autowired
    private KeyProvider keyProvider;

    @DisplayName("then keystores can be loaded from a base64-encoded string using the base64: prefix")
    @Test
    void testLoadKeyStore() {
        assertNotNull(keyStoreProvider.getKeyStore());
        PrivateKey key = keyProvider.getPrivateKey();
        assertNotNull(key);
    }
}
