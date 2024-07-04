package no.idporten.eidas.proxy.crypto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Base64;

@Slf4j
@Getter
public class KeyStoreResourceLoader extends DefaultResourceLoader {

    public KeyStoreResourceLoader() {
        super();
        addBase64ResourceLoader();
    }

    public KeyStoreResourceLoader(ClassLoader classLoader) {
        super(classLoader);
        addBase64ResourceLoader();
    }

    /**
     * Creates resource from base64-encoded strings prefixed with "base64:".
     */
    private void addBase64ResourceLoader() {
        this.addProtocolResolver((location, resourceLoader) -> {
            if (location.startsWith("base64:")) {
                return new ByteArrayResource(decode(location.substring(location.indexOf(":") + 1).trim().getBytes()));
            } else {
                return null;
            }
        });
    }

    private byte[] decode(byte[] bytes) {
        try {
            return Base64.getDecoder().decode(bytes);
        } catch (IllegalArgumentException e) {
            return Base64.getMimeDecoder().decode(bytes);
        }
    }

}