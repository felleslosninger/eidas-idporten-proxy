package no.idporten.eidas.proxy.lightprotocol;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class LevelOfAssurance implements ILevelOfAssurance {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlAttribute(name = "type")
    private String type;
    @XmlAttribute(name = "type")
    private String value;

}
