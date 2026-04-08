package no.idporten.eidas.proxy.integration.idp.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableConfigurationProperties(OIDCIntegrationPropertiesMap.class)
class OIDCIntegrationConfigurationTest {

    @Bean
    OIDCProviderMetadata oidcProviderMetadata() {
        return mock(OIDCProviderMetadata.class);
    }


    @Bean
    IDTokenValidator idTokenValidator() {
        return mock(IDTokenValidator.class);
    }
    @Test
    @DisplayName("Test OIDCProviderMap bean creation (no network)")
    void testOIDCProviderMapBeanCreation() {
        // Prepare an empty properties map to avoid network calls during metadata resolution
        OIDCIntegrationPropertiesMap propertiesMap = new OIDCIntegrationPropertiesMap();
        propertiesMap.setOidcIntegrations(Collections.emptyMap());

        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration(null);
        var providerMap = configuration.oidcProviderMap(propertiesMap);

        assertNotNull(providerMap);
    }

    @Test
    @DisplayName("Test OIDCProviderMap bean creation with local metadata")
    void testOIDCProviderMapBeanCreationWithLocalMetadata() throws Exception {
        OIDCIntegrationProperties properties = new OIDCIntegrationProperties();
        properties.setIssuer(URI.create("https://idporten.dev"));
        properties.setClientId("client-id");
        properties.setMetadataUri(URI.create("classpath:idporten-metadata.json"));
        properties.setJwksCacheLifetimeMinutes(60);
        properties.setJwksCacheRefreshMinutes(5);
        properties.setClientAuthMethod("client_secret_basic");
        properties.setScopes(Collections.singleton("openid"));

        OIDCIntegrationPropertiesMap propertiesMap = new OIDCIntegrationPropertiesMap();
        propertiesMap.setOidcIntegrations(Map.of("idporten", properties));

        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource metadataResource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(metadataResource);

        String metadataJson = "{\"issuer\":\"https://idporten.dev\",\"authorization_endpoint\":\"https://idporten.dev/auth\",\"token_endpoint\":\"https://idporten.dev/token\",\"jwks_uri\":\"https://idporten.dev/jwks\",\"response_types_supported\":[\"code\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"RS256\"]}";
        when(metadataResource.getInputStream()).thenReturn(new ByteArrayInputStream(metadataJson.getBytes(StandardCharsets.UTF_8)));

        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration(resourceLoader);
        var providerMap = configuration.oidcProviderMap(propertiesMap);

        assertNotNull(providerMap);
        assertNotNull(providerMap.getProviders().get("idporten"));
    }

    @Test
    @DisplayName("Test RemoteJWKSet bean creation")
    void testRemoteJWKSetBeanCreation() throws Exception {
        OIDCIntegrationProperties properties = new OIDCIntegrationProperties();
        OIDCProviderMetadata oidcProviderMetadata = mock(OIDCProviderMetadata.class);
        when(oidcProviderMetadata.getJWKSetURI()).thenReturn(new URI("https://example.com/jwks"));

        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration(null);
        JWKSource<SecurityContext> remoteJWKSet = configuration.remoteJWKSet(properties, oidcProviderMetadata);

        assertNotNull(remoteJWKSet);
    }

    @Test
    @DisplayName("Test IDTokenValidator bean creation")
    void testIDTokenValidatorBeanCreation() {
        OIDCIntegrationProperties properties = new OIDCIntegrationProperties();
        properties.setIssuer(URI.create("https://idporten.dev"));
        properties.setClientId("client-id");
        OIDCProviderMetadata oidcProviderMetadata = mock(OIDCProviderMetadata.class);
        when(oidcProviderMetadata.getIDTokenJWSAlgs()).thenReturn(Collections.singletonList(JWSAlgorithm.RS256));
        when(oidcProviderMetadata.getJWKSetURI()).thenReturn(URI.create("https://idporten.dev/jwks"));
        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration(null);
        JWKSource<SecurityContext> remoteJWKSet = mock(JWKSource.class);

        IDTokenValidator idTokenValidator = configuration.idTokenValidator(properties, oidcProviderMetadata, remoteJWKSet);

        assertNotNull(idTokenValidator);
    }


}
