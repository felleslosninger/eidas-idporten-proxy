package no.idporten.eidas.proxy.integration.idp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OIDCIntegrationPropertiesTest {

    @Autowired
    private OIDCIntegrationPropertiesMap propertiesMap;

    @Test
    @DisplayName("Test properties are loaded correctly")
    void testPropertiesLoading() {
        OIDCIntegrationProperties properties = propertiesMap.get("idporten");
        assertAll("Properties Loading",
                () -> assertNotNull(properties, "OIDCIntegrationProperties instance should not be null"),
                () -> assertNotNull(properties.getIssuer(), "Issuer URI should not be null"),
                () -> assertNotNull(properties.getRedirectUri(), "Redirect URI should not be null"),
                () -> assertNotNull(properties.getClientId(), "Client ID should not be null"),
                () -> assertNotNull(properties.getClientSecret(), "Client secret should not be null"),
                () -> assertNotNull(properties.getScopes(), "Scopes must not be null"),
                () -> assertTrue(properties.getConnectTimeoutMillis() > 0, "Connect timeout should be greater than 0"),
                () -> assertTrue(properties.getReadTimeoutMillis() > 0, "Read timeout should be greater than 0"),
                () -> assertTrue(properties.getJwksCacheRefreshMinutes() > 0, "JWKS cache refresh minutes should be greater than 0"),
                () -> assertTrue(properties.getJwksCacheLifetimeMinutes() > 0, "JWKS cache lifetime minutes should be greater than 0"),
                () -> assertEquals("client_secret_basic", properties.getClientAuthMethod(), "Client authentication method should be client_secret_basic")


        );
    }

    @Test
    @DisplayName("Test client secret is required for certain client auth methods")
    void testClientSecretRequired() {
        OIDCIntegrationProperties properties = propertiesMap.get("idporten");
        assertAll("Client Secret Required",
                () -> {
                    properties.setClientAuthMethod("client_secret_basic");
                    properties.setClientSecret(null);
                    assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet, "IllegalArgumentException should be thrown when client secret is null for client_secret_basic");
                },
                () -> {
                    properties.setClientAuthMethod("client_secret_post");
                    properties.setClientSecret(null);
                    assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet, "IllegalArgumentException should be thrown when client secret is null for client_secret_post");
                });
        assertAll("Client Secret Not Required",
                () -> {
                    properties.setClientAuthMethod("private_key_jwt");
                    properties.setClientSecret(null);
                    assertDoesNotThrow(properties::afterPropertiesSet, "No exception should be thrown when client secret is null for private_key_jwt");
                }
        );
    }


}
