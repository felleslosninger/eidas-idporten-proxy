package no.idporten.eidas.proxy.integration.idp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.annotate.JsonIgnore;
import no.idporten.eidas.proxy.crypto.KeystoreProperties;
import no.idporten.sdk.oidcserver.protocol.AuthorizationDetail;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Slf4j
@Validated
public class OIDCIntegrationProperties {

    public static final String RESOURCE = "resource";
    public static final String TYPE = "type";

    @NotNull
    private URI issuer;

    @NotNull
    private URI redirectUri;

    @NotEmpty
    @Pattern(regexp = "^(client_secret_basic|client_secret_post|private_key_jwt)$")
    private String clientAuthMethod;

    @NotEmpty
    private String clientId;

    @NotEmpty
    private Set<String> scopes;

    private String clientSecret;
    private String keyId;

    @Min(1)
    private int connectTimeoutMillis = 5000;
    @Min(1)
    private int readTimeoutMillis = 5000;
    @Min(1)
    private int jwksCacheRefreshMinutes = 5;
    @Min(1)
    private int jwksCacheLifetimeMinutes = 60;

    private List<AuthorizationDetail> authorizationDetails;

    private KeystoreProperties keystore;

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private Set<String> allowedTypeResourceList;


    /**
     * Returns an immutable set of allowed type:resource pairs derived from configuration.
     * Each entry is in the form "<type>:<resource>".
     */
    public Set<String> getConfiguredAuthDetailTypeResourceList() {
        return allowedTypeResourceList != null ? allowedTypeResourceList : Collections.emptySet();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        if (clientAuthMethod.startsWith("client_secret") && !StringUtils.hasText(clientSecret)) {
            notEmptyForClientAuth("client-secret", clientSecret, clientAuthMethod);
        }

        // Build immutable set of allowed "type:resource" pairs at startup
        Set<String> built = new java.util.LinkedHashSet<>();
        if (this.authorizationDetails != null) {
            for (no.idporten.sdk.oidcserver.protocol.AuthorizationDetail cfgAd : this.authorizationDetails) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.convertValue(cfgAd, Map.class);
                String type = (String) map.get(TYPE);
                String resource = (String) map.get(RESOURCE);
                if (type != null && resource != null) {
                    built.add(type + ":" + resource);
                }
            }
        }
        this.allowedTypeResourceList = Collections.unmodifiableSet(built);
    }

    protected void notEmptyForClientAuth(String property, String value, String clientAuthMethod) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(String.format("Property %s must have a value when using client auth method %s", property, clientAuthMethod));
        }
        if (StringUtils.hasText(keyId)) {
            log.info("KeyId (kid) set to {}", keyId);
        }
    }
}
