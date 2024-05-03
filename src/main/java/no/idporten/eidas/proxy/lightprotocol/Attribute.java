package no.idporten.eidas.proxy.lightprotocol;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Attribute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(name = "definition", namespace = "http://cef.eidas.eu/LightRequest")
    private String definition;
}
