package no.idporten.eidas.proxy.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import no.idporten.logging.audit.AuditData;
import no.idporten.logging.audit.AuditDataProvider;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@XmlRootElement(namespace = "http://cef.eidas.eu/LightRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "requestedAttributes")
@EqualsAndHashCode
@Builder
public class LightRequest implements ILightRequest, AuditDataProvider {
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
    public AuditData getAuditData() {
        return AuditData.builder()
                .attributes(createMapForAuditLogging())
                .build();
    }

    private Map<String, Object> createMapForAuditLogging() {
        HashMap<String, Object> all = new HashMap<>();
        all.put("id", id);
        all.put("relay_state", relayState);
        all.put("citizen_country_code", citizenCountryCode);
        all.put("level_of_assurance", levelOfAssurance != null ? levelOfAssurance.getValue() : null);
        all.put("sp_country_code", spCountryCode);
        all.put("attributes", requestedAttributes != null ? requestedAttributes.stream().map(RequestedAttribute::toString).toList() : null);
        all.values().removeIf(Objects::isNull);
        return Map.copyOf(all); // Immutable map throws NPE if values (or keys) is null
    }
}

