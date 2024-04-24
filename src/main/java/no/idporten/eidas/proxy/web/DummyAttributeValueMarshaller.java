package no.idporten.eidas.proxy.web;

import eu.eidas.auth.commons.attribute.AttributeValue;
import eu.eidas.auth.commons.attribute.AttributeValueMarshallingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * Må bare ha denne klassen for å få kjørtforeløpig
 */
@Component
@Slf4j
public class DummyAttributeValueMarshaller implements eu.eidas.auth.commons.attribute.AttributeValueMarshaller {
    @Nonnull
    @Override
    public String marshal(@Nonnull AttributeValue attributeValue) throws AttributeValueMarshallingException {
        log.info("marshalling");
        throw new RuntimeException("Not implemented");
    }

    @Nonnull
    @Override
    public AttributeValue unmarshal(@Nonnull String s, boolean b) throws AttributeValueMarshallingException {
        log.info("unmarshalling");
        throw new RuntimeException("Not implemented");
    }
}
