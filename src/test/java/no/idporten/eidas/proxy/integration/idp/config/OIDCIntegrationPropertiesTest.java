package no.idporten.eidas.proxy.integration.idp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OIDCIntegrationPropertiesTest {

    @Autowired
    private OIDCIntegrationProperties properties;

    @Test
    @DisplayName("Test properties are loaded correctly")
    void testPropertiesLoading() {
        assertAll("Properties Loading",
                () -> assertNotNull(properties, "OIDCIntegrationProperties instance should not be null"),
                () -> assertNotNull(properties.getIssuer(), "Issuer URI should not be null"),
                () -> assertNotNull(properties.getRedirectUri(), "Redirect URI should not be null"),
                () -> assertNotNull(properties.getClientAuthMethod(), "Client authentication method should not be null"),
                () -> assertNotNull(properties.getClientId(), "Client ID should not be null"),
                () -> assertNotNull(properties.getClientSecret(), "Client secret should not be null"),
                () -> assertNotNull(properties.getScopes(), "Scopes must not be null"),
                () -> assertTrue(properties.getConnectTimeOutMillis() > 0, "Connect timeout should be greater than 0"),
                () -> assertTrue(properties.getReadTimeOutMillis() > 0, "Read timeout should be greater than 0"),
                () -> assertTrue(properties.getJwksCacheRefreshMinutes() > 0, "JWKS cache refresh minutes should be greater than 0"),
                () -> assertTrue(properties.getJwksCacheLifetimeMinutes() > 0, "JWKS cache lifetime minutes should be greater than 0"),
                // Optional fields
                () -> assertNull(properties.getClientKeystoreType(), "Client keystore type should be null"),
                () -> assertNull(properties.getClientKeystoreLocation(), "Client keystore location should be null"),
                () -> assertNull(properties.getClientKeystorePassword(), "Client keystore password should be null"),
                () -> assertNull(properties.getClientKeystoreKeyAlias(), "Client keystore key alias should be null"),
                () -> assertNull(properties.getClientKeystoreKeyPassword(), "Client keystore key password should be null")
        );
    }

    @Test
    @DisplayName("Test client secret is required for certain client auth methods")
    void testClientSecretRequired() {
        assertAll("Client Secret Required",
                () -> {
                    properties.setClientAuthMethod("client_secret_basic");
                    properties.setClientSecret(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client secret is null for client_secret_basic");
                },
                () -> {
                    properties.setClientAuthMethod("client_secret_post");
                    properties.setClientSecret(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client secret is null for client_secret_post");
                });
        assertAll("Client Secret Not Required",
                () -> {
                    properties.setClientAuthMethod("private_key_jwt");
                    properties.setClientKeystoreType("type");
                    properties.setClientKeystoreLocation("location");
                    properties.setClientKeystorePassword("password");
                    properties.setClientKeystoreKeyAlias("alias");
                    properties.setClientKeystoreKeyPassword("password");
                    properties.setClientSecret(null);
                    assertDoesNotThrow(() -> properties.afterPropertiesSet(), "No exception should be thrown when client secret is null for private_key_jwt");
                }
        );
    }

    @Test
    @DisplayName("Test client keystore properties are required for private_key_jwt client auth method")
    void testClientKeystorePropertiesRequired() {
        assertAll("Client Keystore Properties Required",
                () -> {
                    properties.setClientAuthMethod("private_key_jwt");
                    properties.setClientKeystoreType(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client keystore type is null for private_key_jwt");
                },
                () -> {
                    properties.setClientKeystoreLocation(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client keystore location is null for private_key_jwt");
                },
                () -> {
                    properties.setClientKeystorePassword(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client keystore password is null for private_key_jwt");
                },
                () -> {
                    properties.setClientKeystoreKeyAlias(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client keystore key alias is null for private_key_jwt");
                },
                () -> {
                    properties.setClientKeystoreKeyPassword(null);
                    assertThrows(IllegalArgumentException.class, () -> properties.afterPropertiesSet(), "IllegalArgumentException should be thrown when client keystore key password is null for private_key_jwt");
                },
                // All properties are set, no exception should be thrown
                () -> {
                    properties.setClientKeystoreType("type");
                    properties.setClientKeystoreLocation("location");
                    properties.setClientKeystorePassword("password");
                    properties.setClientKeystoreKeyAlias("alias");
                    properties.setClientKeystoreKeyPassword("password");
                    assertDoesNotThrow(() -> properties.afterPropertiesSet(), "No exception should be thrown when all client keystore properties are set for private_key_jwt");
                }
        );
    }
}
