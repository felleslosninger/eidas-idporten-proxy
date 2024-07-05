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
@ActiveProfiles("keystore")
class ClasspathKeyStoreProviderTest {

    @Autowired
    private KeyStoreProvider keyStoreProvider;

    @Autowired
    private KeyProvider keyProvider;

    @DisplayName("then keystores can be loaded from classpath using the classpath: prefix")
    @Test
    void testLoadKeyStore() {
        assertNotNull(keyStoreProvider.getKeyStore());
        PrivateKey key = keyProvider.getPrivateKey();
        assertNotNull(key);
    }
}
