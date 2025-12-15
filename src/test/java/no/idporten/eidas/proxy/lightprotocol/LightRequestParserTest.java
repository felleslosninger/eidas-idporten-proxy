package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.lightprotocol.messages.LightRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightRequestParserTest {
    private String xmlData = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <lightRequest xmlns="http://cef.eidas.eu/LightRequest">
                <citizenCountryCode>NO</citizenCountryCode>
                <id>_NPZeBLAkF3F.U1QYwDaw5u37gDUpkBdFYyxxWQnjpZPfTMIMtM6gpTtsF5wEfT1</id>
                <issuer>http://eidas-demo-ca:8080/EidasNodeConnector/ConnectorMetadata</issuer>
                <levelOfAssurance type="notified">http://eidas.europa.eu/LoA/low</levelOfAssurance>
                <providerName>DEMO-SP-CA</providerName>
                <spType>public</spType>
                <spCountryCode>CA</spCountryCode>
                <relayState>5fdba0b0-4a86-42ab-a477-9d36afabeb44</relayState>
                <requestedAttributes>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName</definition>
                    </attribute>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName</definition>
                    </attribute>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/DateOfBirth</definition>
                    </attribute>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier</definition>
                    </attribute>
                </requestedAttributes>
            </lightRequest>
            """;

    @Test
    void testParseXml() {
        LightRequest lightRequest = assertDoesNotThrow(() -> LightRequestParser.parseXml(xmlData));
        assertNotNull(lightRequest);
        assertDoesNotThrow(lightRequest::toString);
        assertEquals("NO", lightRequest.getCitizenCountryCode());
        assertEquals("CA", lightRequest.getSpCountryCode());
        lightRequest.getRequestedAttributesList().forEach(attribute -> assertNotNull(attribute.getDefinition()));
    }
}
