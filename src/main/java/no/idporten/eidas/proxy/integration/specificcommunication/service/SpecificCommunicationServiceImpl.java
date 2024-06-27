package no.idporten.eidas.proxy.integration.specificcommunication.service;

import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.exceptions.SpecificProxyException;
import no.idporten.eidas.proxy.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.proxy.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.proxy.lightprotocol.LightRequestParser;
import no.idporten.eidas.proxy.lightprotocol.LightResponseToXML;
import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificCommunicationServiceImpl implements SpecificCommunicationService {

    private final RedisCache redisCache;
    private final EidasCacheProperties eidasCacheProperties;

    public ILightRequest getAndRemoveRequest(String lightTokenId, Collection<AttributeDefinition<?>> registry) {
        log.info("getAndRemoveRequest {}", lightTokenId);

        String xmlMessage = (String) redisCache.get(eidasCacheProperties.getLightRequestPrefix(lightTokenId));
        try {
            return LightRequestParser.parseXml(xmlMessage);
        } catch (JAXBException e) {
            log.error("Failed to parse message. We ignore it for now and carry on {}", e.getMessage());
        }
        log.info("Can't parse message yet. We ignore it for now and carry on");
        return null;

    }

    @Override
    public BinaryLightToken putResponse(ILightResponse lightResponse) throws SpecificProxyException {
        String xmlResponse = null;
        try {
            xmlResponse = LightResponseToXML.toXml(lightResponse);
        } catch (JAXBException e) {
            log.error("Failed to convert lightResponse to XML {}", e.getMessage());
            throw new SpecificProxyException("Failed to convert lightResponse to XML", e, LightRequest.builder().relayState(lightResponse.getRelayState()).id(lightResponse.getInResponseToId()).build());
        }
        BinaryLightToken binaryLightToken = BinaryLightTokenHelper
                .createBinaryLightToken(eidasCacheProperties.getResponseIssuerName(),
                        eidasCacheProperties.getResponseSecret(),
                        eidasCacheProperties.getAlgorithm());
        log.info("putResponse {}", binaryLightToken.getToken().getId());
        redisCache.set(eidasCacheProperties.getLightResponsePrefix(binaryLightToken.getToken().getId()), xmlResponse, Duration.ofSeconds(eidasCacheProperties.getLightResponseLifetimeSeconds()));
        return binaryLightToken;
    }

    @Override
    public ILightResponse getAndRemoveResponse(String lightTokenId, Collection<AttributeDefinition<?>> registry) throws SpecificProxyException {
        log.info("getAndRemoveResponse {}", lightTokenId);
        return (ILightResponse) redisCache.get(eidasCacheProperties.getLightResponsePrefix(lightTokenId));
    }

    /**
     * eidas-idporten-proxy does not send lightrequests
     */
    @Override
    public BinaryLightToken putRequest(ILightRequest iLightRequest) throws SpecificProxyException {

        throw new UnsupportedOperationException("Not relevant for this service implementation");

    }
}
