package no.idporten.eidas.proxy.integration.specificcommunication.config;


import no.idporten.eidas.proxy.integration.specificcommunication.caches.LightningTokenRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.LightningTokenResponseCache;
import no.idporten.eidas.proxy.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.proxy.integration.specificcommunication.service.RedisCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class CorrelationMapConfiguration {
    @Bean("idpTokenRequestCache")
    public OIDCRequestCache specificMSIdpRequestCorrelationMap(RedisCache redisCache) {
        return new OIDCRequestCache(redisCache);
    }

    @Bean("lightningTokenRequestCache")
    public LightningTokenRequestCache tokenRequestCorrelationMap(RedisCache redisCache) {
        return new LightningTokenRequestCache(redisCache);
    }

    @Bean("lightningTokenResponseCache")
    public LightningTokenResponseCache tokenResponseCorrelationMap(RedisCache redisCache) {
        return new LightningTokenResponseCache(redisCache);
    }
}
