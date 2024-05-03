package no.idporten.eidas.proxy.lightprotocol;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;

public class LightRequestParser {
    public static LightRequest parseXml(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(LightRequest.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (LightRequest) unmarshaller.unmarshal(new StringReader(xml));
    }


}
