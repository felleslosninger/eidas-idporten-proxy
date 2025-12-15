package no.idporten.eidas.proxy.lightprotocol.messages;

import eu.eidas.auth.commons.light.IResponseStatus;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

@XmlRootElement(namespace = "http://cef.eidas.eu/LightResponse")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Status implements IResponseStatus {
    @Serial
    private static final long serialVersionUID = 1L;

    private String statusCode;

    private String statusMessage;

    private String subStatusCode;

    private boolean failure;

    @NotNull
    @Override
    public String getStatusCode() {
        return statusCode;
    }

    @Nullable
    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Nullable
    @Override
    public String getSubStatusCode() {
        return subStatusCode;
    }

    @Override
    public boolean isFailure() {
        return failure;
    }
}
