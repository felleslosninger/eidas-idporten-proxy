package no.idporten.eidas.proxy.lightprotocol;

import no.idporten.eidas.proxy.lightprotocol.messages.*;
import no.idporten.eidas.proxy.logging.AuditIdPattern;
import no.idporten.logging.audit.AuditEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LightRequestTest {

    @Test
    public void getAuditEntryWhenEmtpyThenDoesNotFailAndLoggesEmptyRequest() {
        AuditEntry auditEntry = new LightRequest().getAuditEntry();
        assertNotNull(auditEntry);
        assertEquals(AuditIdPattern.EIDAS_LIGHT_REQUEST.auditIdentifier().auditId(), auditEntry.getAuditId().auditId());
        assertEquals(1, auditEntry.getAttributes().size());
        assertNotNull(auditEntry.getAttribute("light_request"));
        Map lightRequest = (Map) auditEntry.getAttribute("light_request");
        assertEquals(0, lightRequest.size());

    }

    @Test
    public void getAuditEntryWhenRequestHasAll6AttributesThenLoggesAllAttributes() {
        AuditEntry auditEntry = createLightRequest().getAuditEntry();
        assertNotNull(auditEntry);
        assertEquals(AuditIdPattern.EIDAS_LIGHT_REQUEST.auditIdentifier().auditId(), auditEntry.getAuditId().auditId());
        assertEquals(1, auditEntry.getAttributes().size());
        assertNotNull(auditEntry.getAttribute("light_request"));
        Map lightRequest = (Map) auditEntry.getAttribute("light_request");
        assertEquals(6, lightRequest.size());
    }

    private static LightRequest createLightRequest() {
        return LightRequest.builder()
                .id("id")
                .requestedAttributes(List.of(new RequestedAttribute()))
                .relayState("state")
                .citizenCountryCode("CA")
                .spCountryCode("CB")
                .levelOfAssurance((LevelOfAssurance) LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_HIGH))
                .build();
    }


}
