package no.idporten.eidas.proxy.crypto;

public class IDPortenKeyStoreException extends RuntimeException {
    public IDPortenKeyStoreException(String message) {
        super(message);
    }

    public IDPortenKeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
