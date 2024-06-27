package no.idporten.eidas.proxy.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.light.IResponseStatus;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import no.idporten.eidas.proxy.logging.AuditIdPattern;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditEntryProvider;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@XmlRootElement(name = "lightResponse", namespace = "http://cef.eidas.eu/LightResponse")
@XmlType
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
public class LightResponse implements ILightResponse, AuditEntryProvider {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement
    private String citizenCountryCode;
    @XmlElement
    private String id;
    @XmlElement
    private String issuer;
    @XmlElement
    private String levelOfAssurance;
    @XmlElement
    private String relayState;

    @XmlElement
    private String ipAddress;

    @XmlElement
    private String inResponseToId;

    @XmlElement
    private String consent;

    private String subject;

    @XmlElement
    private String subjectNameIdFormat;

    @XmlElement(name = "status", namespace = "http://cef.eidas.eu/LightResponse")
    private Status status;

    @XmlElementWrapper(name = "attributes", namespace = "http://cef.eidas.eu/LightResponse")
    @XmlElement(name = "attribute", namespace = "http://cef.eidas.eu/LightResponse")
    @Singular
    private List<Attribute> attributes;

    @Nonnull
    @Override
    public ImmutableAttributeMap getAttributes() {
        return ImmutableAttributeMap.builder().build();
    }

    @Nullable
    @Override
    public String getIPAddress() {
        return ipAddress;
    }

    @Nonnull
    @Override
    public IResponseStatus getStatus() {
        return status;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return List.of(new LevelOfAssurance("notified", levelOfAssurance));
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public String getLevelOfAssurance() {
        return levelOfAssurance;
    }

    @Nonnull
    public String getIssuer() {
        return issuer;
    }

    @Override
    public AuditEntry getAuditEntry() {
        return AuditEntry.builder()
                .auditId(AuditIdPattern.EIDAS_LIGHT_RESPONSE.auditIdentifier())
                .attribute("light_response", createMapForAuditLogging())
                .build();
    }

    private Map<String, Object> createMapForAuditLogging() {
        HashMap<String, Object> all = new HashMap<>();
        all.put("id", id);
        all.put("issuer", issuer);
        all.put("status", status);
        all.put("in_response_to_id", inResponseToId);
        all.put("relay_state", relayState);
        all.put("citizen_country_code", citizenCountryCode);
        all.put("level_of_assurance", levelOfAssurance);
        all.put("sub", subject);
        all.put("attributes", attributes);
        all.values().removeIf(Objects::isNull);
        return Map.copyOf(all); // Immutable map throws NPE if values (or keys) is null
    }

}

