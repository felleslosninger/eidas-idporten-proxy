package no.idporten.eidas.proxy.integration.specificcommunication.config;


import eu.eidas.auth.commons.light.ILightResponse;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.LightningTokenResponseCache;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.service.RedisCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class CorrelationMapConfiguration {
    @Bean("idpTokenRequestCache")
    public OIDCRequestCache specificMSIdpRequestCorrelationMap(RedisCache<String, CorrelatedRequestHolder> redisCache, EidasCacheProperties eidasCacheProperties) {
        return new OIDCRequestCache(redisCache, eidasCacheProperties.getOidcRequestStateLifetimeSeconds());
    }

    @Bean("lightningTokenResponseCache")
    public LightningTokenResponseCache tokenResponseCorrelationMap(RedisCache<String, ILightResponse> redisCache, EidasCacheProperties eidasCacheProperties) {
        return new LightningTokenResponseCache(redisCache, eidasCacheProperties.getLightResponseLifetimeSeconds());
    }
}
