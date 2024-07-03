package no.idporten.eidas.proxy.lightprotocol;


import eu.eidas.auth.commons.light.ILightResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;

import java.io.StringWriter;

public class LightResponseToXML {
    public static String toXml(ILightResponse lightResponse) throws JAXBException {
        if (lightResponse == null) {
            throw new IllegalArgumentException("The lightResponse object cannot be null.");
        }
        JAXBContext context = JAXBContext.newInstance(LightResponse.class);

        Marshaller marshaller = context.createMarshaller();
        marshaller.setEventHandler(new DefaultValidationEventHandler());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(lightResponse, writer);
        return writer.toString();
    }

    private LightResponseToXML() {
    }

}
