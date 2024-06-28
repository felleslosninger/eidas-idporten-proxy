package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import no.idporten.logging.audit.AuditData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightResponseTest {

    @Test
    void getAuditDataWhenEmtpyThenDoesNotFailAndLoggesEmptyResponse() {
        AuditData auditData = new LightResponse().getAuditData();
        assertNotNull(auditData);
        assertNotNull(auditData.getAttributes());
        assertEquals(0, auditData.getAttributes().size());

    }

    @Test
    void getAuditDataWhenResponseHasAll9AttributesThenLoggesAllAttributes() {
        AuditData auditData = createLightResponse().getAuditData();
        assertNotNull(auditData);
        assertNotNull(auditData.getAttributes());
        assertEquals(9, auditData.getAttributes().size());
        assertInstanceOf(String.class, auditData.getAttributes().get("attributes"));
        assertInstanceOf(String.class, auditData.getAttributes().get("level_of_assurance"));
    }

    private static LightResponse createLightResponse() {
        return LightResponse.builder()
                .subject("sub")
                .inResponseToId("responseTo")
                .id("id")
                .attribute(new Attribute())
                .relayState("state")
                .citizenCountryCode("CA")
                .issuer("http://dd")
                .levelOfAssurance("A")
                .status(Status.builder().statusCode("ok").build())
                .build();
    }


}
