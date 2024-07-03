package no.idporten.eidas.proxy.integration.idp.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.HashSet;

@Slf4j
@Configuration
@EnableConfigurationProperties(OIDCIntegrationProperties.class)
public class OIDCIntegrationConfiguration {

    @Bean
    public OIDCProviderMetadata oidcProviderMetadata(OIDCIntegrationProperties properties) throws Exception {
        Issuer issuer = new Issuer(properties.getIssuer());
        OIDCProviderMetadata oidcProviderMetadata = OIDCProviderMetadata.resolve(
                issuer,
                properties.getConnectTimeOutMillis(),
                properties.getReadTimeOutMillis());
        log.info("Read OpenID Connect metadata with configuration from issuer {}", issuer);
        return oidcProviderMetadata;
    }

    @Bean
    public JWKSource<SecurityContext> remoteJWKSet(OIDCIntegrationProperties properties, OIDCProviderMetadata oidcProviderMetadata) throws MalformedURLException {
        return JWKSourceBuilder.create(oidcProviderMetadata.getJWKSetURI().toURL())
                .rateLimited(true)
                .cache(Duration.ofMinutes(properties.getJwksCacheLifetimeMinutes()).toMillis(),
                        Duration.ofMinutes(properties.getJwksCacheRefreshMinutes()).toMillis())
                .build();

    }

    @Bean
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
