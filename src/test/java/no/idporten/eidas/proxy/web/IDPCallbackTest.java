package no.idporten.eidas.proxy.web;

import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.impl.LightRequest;
import eu.eidas.auth.commons.light.impl.LightResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IDPCallbackTest {


    @Test
    @DisplayName("when buildLightResponse then return LightResponse without validation errors")
    void buildLightResponse() {
        IDTokenClaimsSet idTokenClaimsSet = new IDTokenClaimsSet(new Issuer("http://myproxy"),
                new Subject("bob"),
                List.of(new Audience("junit")),
                Date.from(Instant.now()),
                Date.from(Instant.now()));
        idTokenClaimsSet.setClaim("family_name", "Smith");

        ILightRequest lightRequest = LightRequest.builder()
                .id("myid")
                .issuer("http://euproxy")
                .levelOfAssurance("weirdprefix:eidas-loa-high")
                .relayState("myrelaystate")
                .citizenCountryCode("NO")
                .build();


        LightResponse lightResponse = IDPCallback.getLightResponse(idTokenClaimsSet, lightRequest);
        assertNotNull(lightResponse);
    }
}
