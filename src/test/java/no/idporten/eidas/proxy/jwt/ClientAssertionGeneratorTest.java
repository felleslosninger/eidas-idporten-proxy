package no.idporten.eidas.proxy.jwt;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("When generating a jwt grant")
@ActiveProfiles("keystore")
class ClientAssertionGeneratorTest {

    @Autowired
    private ClientAssertionGenerator clientAssertionGenerator;

    @Test
    @DisplayName("the jwt header must contain kid if using kid")
    void testGenerateJwtWithKid() {
        ClientAuthentication s = clientAssertionGenerator.create();
        assertNotNull(s);
        assertInstanceOf(PrivateKeyJWT.class, s);
        assertEquals("okcomputer", ((PrivateKeyJWT) s).getClientAssertion().getHeader().getKeyID());
    }

    @Test
    @DisplayName("the jwt grant must contain a jwt claimset")
    void testGenerateJwt() throws ParseException {
        SignedJWT s = ((PrivateKeyJWT) clientAssertionGenerator.create()).getClientAssertion();
        assertNotNull(s);
        assertNotNull(s.getJWTClaimsSet());
    }

}
