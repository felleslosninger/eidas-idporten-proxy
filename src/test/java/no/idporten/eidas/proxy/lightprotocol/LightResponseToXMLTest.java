package no.idporten.eidas.proxy.lightprotocol;

import jakarta.xml.bind.JAXBException;
import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightResponseToXMLTest {

    @Test
    void testSerializeToXML() {
        LightResponse lightResponse = LightResponse.builder()
                .inResponseToId("123")
                .levelOfAssurance(new LevelOfAssurance("type", "value"))
                .subjectNameIdFormat("format")
                .issuer("issuer")
                .status(new Status("200", "ok", null, false))
                .ipAddress("123.12.12.12")
                .consent("consent")
                .attributes(List.of(new Attribute("name", List.of("value"))))
                .citizenCountryCode("NO")
                .relayState("123")
                .build();
        try {
            String xml = LightResponseToXML.toXml(lightResponse);
            assertNotNull(xml);
        } catch (JAXBException e) {
            fail("Failed to serialize to XML %s : %s: %s", e.getErrorCode(), e.getMessage(), e.getCause());
            ;
        }
    }
}
