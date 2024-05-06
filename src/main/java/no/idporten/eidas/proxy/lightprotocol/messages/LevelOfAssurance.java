package no.idporten.eidas.proxy.lightprotocol.messages;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class LevelOfAssurance implements ILevelOfAssurance {
    @Serial
    private static final long serialVersionUID = 1L;
    private String type;
    private String value;

}
