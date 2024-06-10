package no.idporten.eidas.proxy.service;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When checking level of assurance")
@SpringBootTest
class LevelOfAssuranceHelperTest {

    @Autowired
    private LevelOfAssuranceHelper levelOfAssuranceHelper;

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


