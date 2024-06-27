package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.lightprotocol.messages.Attribute;
import no.idporten.eidas.proxy.lightprotocol.messages.LightResponse;
import no.idporten.eidas.proxy.lightprotocol.messages.Status;
import no.idporten.eidas.proxy.logging.AuditIdPattern;
import no.idporten.logging.audit.AuditEntry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LightResponseTest {

    @Test
    public void getAuditEntryWhenEmtpyThenDoesNotFailAndLoggesEmptyResponse() {
        AuditEntry auditEntry = new LightResponse().getAuditEntry();
        assertNotNull(auditEntry);
        assertEquals(AuditIdPattern.EIDAS_LIGHT_RESPONSE.auditIdentifier().auditId(), auditEntry.getAuditId().auditId());
        assertEquals(1, auditEntry.getAttributes().size());
        assertNotNull(auditEntry.getAttribute("light_response"));
        Map lightResponse = (Map) auditEntry.getAttribute("light_response");
        assertEquals(0, lightResponse.size());

    }

    @Test
    public void getAuditEntryWhenResponseHasAll9AttributesThenLoggesAllAttributes() {
        AuditEntry auditEntry = createLightResponse().getAuditEntry();
        assertNotNull(auditEntry);
        assertEquals(AuditIdPattern.EIDAS_LIGHT_RESPONSE.auditIdentifier().auditId(), auditEntry.getAuditId().auditId());
        assertEquals(1, auditEntry.getAttributes().size());
        assertNotNull(auditEntry.getAttribute("light_response"));
        Map lightResponse = (Map) auditEntry.getAttribute("light_response");
        assertEquals(9, lightResponse.size());
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
