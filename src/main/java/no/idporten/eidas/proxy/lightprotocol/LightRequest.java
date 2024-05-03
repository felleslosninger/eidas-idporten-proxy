package no.idporten.eidas.proxy.lightprotocol;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.xml.bind.annotation.*;
import lombok.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serial;
import java.util.List;

@XmlRootElement(namespace = "http://cef.eidas.eu/LightRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "requestedAttributes")
@EqualsAndHashCode
public class LightRequest implements ILightRequest {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String citizenCountryCode;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String id;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String issuer;
    @XmlElement(name = "levelOfAssurance", namespace = "http://cef.eidas.eu/LightRequest")
    private LevelOfAssurance levelOfAssurance;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String providerName;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String spType;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String spCountryCode;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String relayState;
    @XmlElementWrapper(name = "requestedAttributes", namespace = "http://cef.eidas.eu/LightRequest")
    @XmlElement(name = "attribute", namespace = "http://cef.eidas.eu/LightRequest")
    private List<Attribute> requestedAttributes;

    public List<ILevelOfAssurance> getLevelsOfAssurance() {
        return List.of(levelOfAssurance);
    }

    @Nullable
    public String getNameIdFormat() {
        return null;
    }

    @Nullable
    public String getRequesterId() {
        return id;
    }

    @Nonnull
    public ImmutableAttributeMap getRequestedAttributes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<Attribute> getRequestedAttributesList() {
        return requestedAttributes;
    }

    public String getLevelOfAssurance() {
        return levelOfAssurance.getValue();
    }


}

