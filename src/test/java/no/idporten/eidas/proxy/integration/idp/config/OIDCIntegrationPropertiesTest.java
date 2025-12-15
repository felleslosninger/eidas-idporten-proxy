package no.idporten.eidas.proxy.integration.idp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.sdk.oidcserver.protocol.AuthorizationDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OIDCIntegrationPropertiesTest {

    @Autowired
    private OIDCIntegrationPropertiesMap propertiesMap;

    private static final ObjectMapper MAPPER = new ObjectMapper();

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


    private static OIDCIntegrationProperties newPropsWithDefaults(List<AuthorizationDetail> ads) throws Exception {
        OIDCIntegrationProperties props = new OIDCIntegrationProperties();
        props.setClientAuthMethod("client_secret_basic");
        props.setClientSecret("secret");
        props.setClientId("client-id");
        props.setIssuer(new URI("https://issuer.example"));
        props.setRedirectUri(new URI("https://client.example/cb"));
        props.setScopes(Set.of("openid"));
        props.setAuthorizationDetails(ads);
        props.afterPropertiesSet();
        return props;
    }

    private static AuthorizationDetail ad(String type, String resource) {
        Map<String, Object> map = Map.of(
                OIDCIntegrationProperties.TYPE, type,
                OIDCIntegrationProperties.RESOURCE, resource
        );
        return MAPPER.convertValue(map, AuthorizationDetail.class);
    }

    @Test
    @DisplayName("allowed type:resource list is built at PostConstruct and is immutable")
    void buildsImmutableTypeResourceList() throws Exception {
        OIDCIntegrationProperties props = newPropsWithDefaults(List.of(
                ad("t1", "r1")
        ));

        Set<String> allowed = props.getConfiguredAuthDetailTypeResourceList();
        assertEquals(1, allowed.size());
        assertTrue(allowed.contains("t1:r1"));
        assertThrows(UnsupportedOperationException.class, () -> allowed.add("x:y"));
    }

    @Test
    @DisplayName("duplicate types with different resources are all kept in the list")
    void duplicateTypeKeepsAllResources() throws Exception {
        OIDCIntegrationProperties props = newPropsWithDefaults(List.of(
                ad("t1", "r-first"),
                ad("t1", "r-second")
        ));

        Set<String> allowed = props.getConfiguredAuthDetailTypeResourceList();
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains("t1:r-first"));
        assertTrue(allowed.contains("t1:r-second"));
    }

    @Test
    @DisplayName("null or empty configuration yields empty type:resource list")
    void emptyConfigYieldsEmptyMap() throws Exception {
        OIDCIntegrationProperties propsNull = newPropsWithDefaults(null);
        assertTrue(propsNull.getConfiguredAuthDetailTypeResourceList().isEmpty());

        OIDCIntegrationProperties propsEmpty = newPropsWithDefaults(List.of());
        assertTrue(propsEmpty.getConfiguredAuthDetailTypeResourceList().isEmpty());
    }
}
