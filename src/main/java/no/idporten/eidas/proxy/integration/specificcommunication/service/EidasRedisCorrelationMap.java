package no.idporten.eidas.proxy.integration.specificcommunication.service;

import eu.eidas.auth.commons.light.ILightRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EidasRedisCorrelationMap extends AbstractCorrelationMap<ILightRequest> {

    EidasRedisCorrelationMap(RedisCache redisCache) {
        super(redisCache);
    }
}
