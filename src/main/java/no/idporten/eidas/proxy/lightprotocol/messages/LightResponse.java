package no.idporten.eidas.proxy.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.light.IResponseStatus;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import no.idporten.eidas.proxy.logging.AuditIdPattern;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditEntryProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.util.List;
import java.util.Map;

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
                .attribute("light_response", Map.of(
                        "id", id,
                        "issuer", issuer,
                        "status", status,
                        "in_response_to_id", inResponseToId,
                        "relay_state", relayState,
                        "country_code", citizenCountryCode,
                        "level_of_assurance_returned", levelOfAssurance,
                        "sub", subject,
                        "attributes", attributes
                ))
                .build();
    }

}

