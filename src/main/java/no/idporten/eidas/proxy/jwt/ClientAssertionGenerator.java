package no.idporten.eidas.proxy.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.crypto.KeyProvider;
import no.idporten.eidas.proxy.integration.idp.OIDCProviders;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;
import no.idporten.eidas.proxy.integration.idp.exceptions.OAuthException;
import org.springframework.stereotype.Service;

import java.security.cert.Certificate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
public class ClientAssertionGenerator {

    private final OIDCProviders oidcProviders;

    public ClientAuthentication create(String idp) {
        try {
            OIDCIntegrationProperties properties = oidcProviders.get(idp).getProperties();
            KeyProvider keyProvider = requireNonNull(oidcProviders.get(idp).getKeyProvider());
            List<Base64> encodedCertificates = new ArrayList<>();
            for (Certificate c : keyProvider.getCertificateChain()) {
                encodedCertificates.add(Base64.encode(c.getEncoded()));
            }
            JWSHeader header = new JWSHeader
                    .Builder(JWSAlgorithm.RS256)
                    .x509CertChain(encodedCertificates)
                    .keyID(properties.getKeyId())
                    .build();
            long created = Clock.systemUTC().millis();
            long expires = created + (120 * 1000L);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.getClientId())
                    .subject(properties.getClientId())
                    .audience(properties.getIssuer().toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(new Date(created))
                    .expirationTime(new Date(expires))
                    .build();
            JWSSigner signer = new RSASSASigner(keyProvider.getPrivateKey());
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);
            return new PrivateKeyJWT(signedJWT);
        } catch (Exception e) {
            throw new OAuthException("Failed to create client assertion " + e.getMessage());
        }
    }
}
