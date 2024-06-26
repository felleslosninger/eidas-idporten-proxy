package no.idporten.eidas.proxy.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditEntryProvider;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.util.List;

@XmlRootElement(namespace = "http://cef.eidas.eu/LightRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "requestedAttributes")
@EqualsAndHashCode
@Builder
public class LightRequest implements ILightRequest, AuditEntryProvider {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String citizenCountryCode;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String id;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String issuer;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private LevelOfAssurance levelOfAssurance;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String relayState;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String providerName;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String spType;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String nameIdFormat;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String requesterId;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String spCountryCode;
    @XmlElementWrapper(name = "requestedAttributes", namespace = "http://cef.eidas.eu/LightRequest")
    @XmlElement(name = "attribute", namespace = "http://cef.eidas.eu/LightRequest")
    private List<RequestedAttribute> requestedAttributes;


    @Nonnull
    public ImmutableAttributeMap getRequestedAttributes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<RequestedAttribute> getRequestedAttributesList() {
        return requestedAttributes;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    public String getLevelOfAssurance() {
        return levelOfAssurance.getValue();
    }

    @Override
    public List<ILevelOfAssurance> getLevelsOfAssurance() {
        return List.of(levelOfAssurance);
    }

    @Nonnull
    public String getIssuer() {
        return issuer;
    }

    @Override
    public AuditEntry getAuditEntry() {
        return AuditEntry.builder()
                .attribute("id", id)
                .attribute("relay_state", relayState)
                .attribute("citizen_country_code", citizenCountryCode)
                .attribute("level_of_assurance", levelOfAssurance)
                .attribute("sp_country_code", spCountryCode)
                .attribute("requested_attributes", requestedAttributes)
                .build();
    }
}

