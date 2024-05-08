package no.idporten.eidas.proxy.integration.idp.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@EnableConfigurationProperties(OIDCIntegrationProperties.class)
class OIDCIntegrationConfigurationTest {

    @Bean
    OIDCProviderMetadata oidcProviderMetadata() {
        return mock(OIDCProviderMetadata.class);
    }

    @Bean
    RemoteJWKSet remoteJWKSet() {
        return mock(RemoteJWKSet.class);
    }

    @Bean
    IDTokenValidator idTokenValidator() {
        return mock(IDTokenValidator.class);
    }
    @Test
    @DisplayName("Test OIDCProviderMetadata bean creation")
    void testOIDCProviderMetadataBeanCreation() throws Exception {
        final String issuerUri = "https://idporten.dev";
        OIDCIntegrationProperties properties = new OIDCIntegrationProperties();
        properties.setIssuer(new URI(issuerUri));
        properties.setConnectTimeOutMillis(5000);
        properties.setReadTimeOutMillis(5000);

        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration();
        OIDCProviderMetadata oidcProviderMetadata = configuration.oidcProviderMetadata(properties);


        Issuer issuer = new Issuer(issuerUri);
        assertEquals(issuer, oidcProviderMetadata.getIssuer());
    }

    @Test
    @DisplayName("Test RemoteJWKSet bean creation")
    void testRemoteJWKSetBeanCreation() throws Exception {
        OIDCIntegrationProperties properties = new OIDCIntegrationProperties();
        OIDCProviderMetadata oidcProviderMetadata = mock(OIDCProviderMetadata.class);
        when(oidcProviderMetadata.getJWKSetURI()).thenReturn(new URI("https://example.com/jwks"));

        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration();
        RemoteJWKSet remoteJWKSet = configuration.remoteJWKSet(properties, oidcProviderMetadata);


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
        OIDCIntegrationConfiguration configuration = new OIDCIntegrationConfiguration();
        RemoteJWKSet remoteJWKSet = mock(RemoteJWKSet.class);

        IDTokenValidator idTokenValidator = configuration.idTokenValidator(properties, oidcProviderMetadata, remoteJWKSet);

        assertNotNull(idTokenValidator);
    }


}
