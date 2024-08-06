package no.idporten.eidas.proxy.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("keystore")
class KeystorePropertiesTest {

    @Autowired
    private KeystoreProperties properties;

    @Test
    @DisplayName("Test properties are loaded correctly")
    void testPropertiesSet() {
        assertNotNull(properties, "KeystoreProperties instance should not be null");
    }

    @Test
    @DisplayName("Test keystore properties are required")
    void testClientKeystorePropertiesRequired() {
        assertAll("Client Keystore Properties Required",
                // All properties are set, no exception should be thrown
                () -> {
                    properties.setKeystoreType("type");
                    properties.setKeystoreLocation("location");
                    properties.setKeystorePassword("password");
                    properties.setKeyAlias("alias");
                    properties.setKeyPassword("password");
                    assertDoesNotThrow(() -> properties.afterPropertiesSet(), "No exception should be thrown when all client keystore properties are set for private_key_jwt");
                }
        );
    }
}
