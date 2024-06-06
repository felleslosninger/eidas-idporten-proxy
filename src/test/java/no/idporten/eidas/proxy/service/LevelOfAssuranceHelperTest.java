package no.idporten.eidas.proxy.service;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import no.idporten.eidas.proxy.config.AcrProperties;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("When checking level of assurance")
@ExtendWith(SpringExtension.class)
class LevelOfAssuranceHelperTest {

    @Mock
    private AcrProperties acrPropertiesMock;
    @InjectMocks
    private LevelOfAssuranceHelper levelOfAssuranceHelper;

    @BeforeEach
    void setup() {
        when(acrPropertiesMock.getSupportedAcrValues()).thenReturn(List.of("http://eidas.europa.eu/LoA/low",
                "http://eidas.europa.eu/LoA/substantial",
                "http://eidas.europa.eu/LoA/high"));
        when(acrPropertiesMock.getAcrValueMapFromIdporten()).thenReturn(Map.of("idporten-loa-low", "http://eidas.europa.eu/LoA/low",
                "idporten-loa-substantial", "http://eidas.europa.eu/LoA/substantial",
                "idporten-loa-high", "http://eidas.europa.eu/LoA/high"));

        when(acrPropertiesMock.getAcrValueMapToIdporten()).thenReturn(Map.of(
                "http://eidas.europa.eu/LoA/low", "no-notified-low",
                "http://eidas.europa.eu/LoA/substantial", "no-notified-substantial",
                "http://eidas.europa.eu/LoA/high", "no-notified-high"
        ));

    }

    @Test
    @DisplayName("then substantial must be allowed if requested value was substantial and high")
    void testValidAcrsubstantialWhensubstantialAndhighRequested() {
        assertTrue(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/substantial", List.of("http://eidas.europa.eu/LoA/high", "http://eidas.europa.eu/LoA/substantial")));
    }

    @Test
    @DisplayName("then substantial must be allowed if requested value was substantial")
    void testValidAcrsubstantialWhenMinimum3Requested() {
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/substantial"));
    }

    @Test
    @DisplayName("then all levels must be allowed if acr was not requested")
    void testValidAcrsubstantialAndHighWhenNoLevelRequested() {
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/low"));
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/substantial"));
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/high"));
    }

    @Test
    @DisplayName("then high must be allowed if requested value was substantial ")
    void testValidAcrLevel() {
        assertTrue(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/high", List.of("http://eidas.europa.eu/LoA/substantial")));
    }

    @Test
    @DisplayName("then substantial must not be allowed if requested value was high")
    void testInvalidAcrLevel() {
        assertFalse(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/substantial", List.of("http://eidas.europa.eu/LoA/high")));
    }

    @Test
    @DisplayName("then high must be allowed if minimum requested value was high")
    void testValidAcrhighWhenMinimumHighRequested() {
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/high"));
    }

    @Test
    @DisplayName("then if the request acr is null, but supported, it is valid")
    void testNullAcrsForEid() {
        assertTrue(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/low", null));
    }

    @Test
    void testAcrValueFromEidasToIdportenMapping() {
        List<String> idportenAcr = levelOfAssuranceHelper.eidasAcrListToIdportenAcrList(List.of(
                LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_LOW),
                LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_SUBSTANTIAL),
                LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_HIGH)));
        assertEquals(3, idportenAcr.size());
        assertEquals(List.of("no-notified-low", "no-notified-substantial", "no-notified-high"), idportenAcr);
    }

    @Test
    void testAcrValueFromIdportenToEidasMapping() {
        ILevelOfAssurance eidasAcr = levelOfAssuranceHelper.idportenAcrListToEidasAcr("idporten-loa-high");
        assertNotNull(eidasAcr);
        assertEquals(LevelOfAssurance.EIDAS_LOA_HIGH, eidasAcr.getValue());
    }

}


