package no.idporten.eidas.proxy.integration.idp;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.crypto.KeyProvider;
import no.idporten.eidas.proxy.integration.idp.config.OIDCIntegrationProperties;

import javax.annotation.Nullable;

@Getter
@RequiredArgsConstructor
public class OIDCProvider {
    private final String id; // e.g. idporten, ansattporten
    private final OIDCIntegrationProperties properties;
    private final OIDCProviderMetadata metadata;
    private final JWKSource<SecurityContext> jwkSource;
    private final IDTokenValidator idTokenValidator;
    @Nullable
    private final KeyProvider keyProvider;

}
