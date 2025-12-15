package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LightResponseToXMLTest {

    @Test
    void testSerializeToXML() {
        LightResponse lightResponse = LightResponse.builder()
                .inResponseToId("123")
                .levelOfAssurance("low")
                .subject("123")
                .subjectNameIdFormat("format")
                .issuer("issuer")
                .status(new Status("200", "ok", null, false))
                .ipAddress("123.12.12.12")
                .consent("consent")
                .attributes(List.of(new Attribute("name", List.of("value"))))
                .citizenCountryCode("NO")
                .relayState("123")
                .build();
        String xml = assertDoesNotThrow(() -> LightResponseToXML.toXml(lightResponse));
        assertNotNull(xml);
        assertFalse(xml.contains("ns2:"), "should not include any namespace prefix (ns2:)");
    }
}
