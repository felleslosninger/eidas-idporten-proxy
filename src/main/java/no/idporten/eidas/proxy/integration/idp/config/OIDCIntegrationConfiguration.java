package no.idporten.eidas.proxy.integration.idp.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.crypto.KeyProvider;
import no.idporten.eidas.proxy.crypto.KeyStoreProvider;
import no.idporten.eidas.proxy.crypto.KeyStoreResourceLoader;
import no.idporten.eidas.proxy.crypto.KeystoreProperties;
import no.idporten.eidas.proxy.integration.idp.OIDCProvider;
import no.idporten.eidas.proxy.integration.idp.OIDCProviders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties(OIDCIntegrationPropertiesMap.class)
public class OIDCIntegrationConfiguration {


    @Bean
    public OIDCProviders oidcProviderMap(OIDCIntegrationPropertiesMap propertiesMap) {
        Map<String, OIDCProvider> providers = new HashMap<>();
        propertiesMap.getOidcIntegrations().forEach((id, props) -> {
            Issuer issuer = new Issuer(props.getIssuer());
            try {
                // Resolve metadata per provider
                OIDCProviderMetadata metadata = OIDCProviderMetadata.resolve(
                        issuer,
                        props.getConnectTimeoutMillis(),
                        props.getReadTimeoutMillis());

                log.info("Read OpenID Connect metadata with configuration from issuer {}", issuer);

                // Create a JWKS source per provider
                JWKSource<SecurityContext> jwkSource = JWKSourceBuilder.create(metadata.getJWKSetURI().toURL())
                        .rateLimited(true)
                        .cache(
                                Duration.ofMinutes(props.getJwksCacheLifetimeMinutes()).toMillis(),
                                Duration.ofMinutes(props.getJwksCacheRefreshMinutes()).toMillis()
                        )
                        .build();

                // Create an ID Token validator per provider
                JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                        new HashSet<>(metadata.getIDTokenJWSAlgs()),
                        jwkSource);
                IDTokenValidator validator = new IDTokenValidator(
                        new Issuer(props.getIssuer()),
                        new ClientID(props.getClientId()),
                        keySelector,
                        null);

                KeyProvider keyProvider = null;
                if (props.getClientAuthMethod().equals(ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue())) {
                    keyProvider = loadKeyProvider(props);
                }
                // Assemble provider
                OIDCProvider provider = new OIDCProvider(id, props, metadata, jwkSource, validator, keyProvider);
                //add it to the list
                providers.put(id, provider);
            } catch (GeneralException | IOException e) {
                throw new RuntimeException("Failed to instantiate OIDC provider components for %s".formatted(id), e);
            }
        });
        //then return the finished result
        return new OIDCProviders(providers);

    }

    private static KeyProvider loadKeyProvider(OIDCIntegrationProperties props) {

        KeystoreProperties keystoreProperties = props.getKeystore();
        KeyStoreResourceLoader keyStoreResourceLoader = new KeyStoreResourceLoader();
        KeyStoreProvider keyStoreProvider = new KeyStoreProvider(
                keystoreProperties.getKeystoreType(),
                keystoreProperties.getKeystoreLocation(),
                keystoreProperties.getKeystorePassword(),
                keyStoreResourceLoader);
        return new KeyProvider(
                keyStoreProvider.getKeyStore(),
                keystoreProperties.getKeyAlias(),
                keystoreProperties.getKeyPassword());

    }

    // Helper for tests and potential reuse; not a Spring bean
    public JWKSource<SecurityContext> remoteJWKSet(OIDCIntegrationProperties properties, OIDCProviderMetadata oidcProviderMetadata) throws MalformedURLException {
        return JWKSourceBuilder.create(oidcProviderMetadata.getJWKSetURI().toURL())
                .rateLimited(true)
                .cache(Duration.ofMinutes(properties.getJwksCacheLifetimeMinutes()).toMillis(),
                        Duration.ofMinutes(properties.getJwksCacheRefreshMinutes()).toMillis())
                .build();

    }

    // Helper for tests and potential reuse; not a Spring bean
    public IDTokenValidator idTokenValidator(OIDCIntegrationProperties properties, OIDCProviderMetadata oidcProviderMetadata, JWKSource<SecurityContext> remoteJWKSet) {
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                new HashSet<>(oidcProviderMetadata.getIDTokenJWSAlgs()),
                remoteJWKSet);
        return new IDTokenValidator(
                new Issuer(properties.getIssuer()),
                new ClientID(properties.getClientId()),
                keySelector,
                null);
    }

}
