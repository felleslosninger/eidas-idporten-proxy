package no.idporten.eidas.proxy.crypto;

import no.idporten.eidas.proxy.integration.idp.OIDCProvider;
import no.idporten.eidas.proxy.integration.idp.OIDCProviders;
import no.idporten.eidas.proxy.service.IDPSelector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("When loading keystores")
@SpringBootTest
@ActiveProfiles("keystore")
class ClasspathKeyStoreProviderTest {

    @Autowired
    private OIDCProviders oidcProviders;

    @DisplayName("then keystores can be loaded from classpath using the classpath: prefix")
    @Test
    void testLoadKeyStore() {
        OIDCProvider oidcProvider = oidcProviders.get(IDPSelector.IDPORTEN);
        assertEquals("idporten", oidcProvider.getId());
        assertNotNull(oidcProvider.getKeyProvider());
        PrivateKey key = oidcProvider.getKeyProvider().getPrivateKey();
        assertNotNull(key);
    }
}
