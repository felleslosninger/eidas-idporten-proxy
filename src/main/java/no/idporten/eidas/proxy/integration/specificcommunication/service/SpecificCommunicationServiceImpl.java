package no.idporten.eidas.proxy.integration.specificcommunication.service;

import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificCommunicationServiceImpl implements SpecificCommunicationService {

    private final RedisCache redisCache;
    private final EidasCacheProperties eidasCacheProperties;

    @Override
    public BinaryLightToken putRequest(ILightRequest iLightRequest) throws SpecificCommunicationException {

        BinaryLightToken binaryLightToken = BinaryLightTokenHelper
                .createBinaryLightToken(eidasCacheProperties.getIssuerName(),
                        eidasCacheProperties.getSecret(),
                        eidasCacheProperties.getAlgorithm());

        redisCache.set(eidasCacheProperties.getLightRequestPrefix(binaryLightToken.getToken().getId()), iLightRequest, Duration.ofSeconds(120));
        return binaryLightToken;

    }

    public ILightRequest getAndRemoveRequest(String lightTokenId, Collection<AttributeDefinition<?>> registry) {
        log.info("getAndRemoveRequest {}", lightTokenId);
        return (ILightRequest) redisCache.get(eidasCacheProperties.getLightRequestPrefix(lightTokenId));
    }

    @Override
    public BinaryLightToken putResponse(ILightResponse iLightResponse) throws SpecificCommunicationException {
        BinaryLightToken binaryLightToken = BinaryLightTokenHelper
                .createBinaryLightToken(eidasCacheProperties.getIssuerName(),
                        eidasCacheProperties.getSecret(),
                        eidasCacheProperties.getAlgorithm());
        log.info("putResponse {}", binaryLightToken.getToken().getId());
        redisCache.set(eidasCacheProperties.getLightResponsePrefix(binaryLightToken.getToken().getId()), iLightResponse, Duration.ofSeconds(120));
        return binaryLightToken;
    }

    @Override
    public ILightResponse getAndRemoveResponse(String lightTokenId, Collection<AttributeDefinition<?>> registry) throws SpecificCommunicationException {
        log.info("getAndRemoveResponse {}", lightTokenId);
        return (ILightResponse) redisCache.get(eidasCacheProperties.getLightResponsePrefix(lightTokenId));
    }


}
