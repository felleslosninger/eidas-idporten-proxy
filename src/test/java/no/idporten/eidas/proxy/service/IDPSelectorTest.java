package no.idporten.eidas.proxy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static no.idporten.eidas.proxy.service.EidasAttributeNames.E_JUSTICE_NATURAL_PERSON_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("IDPSelector")
class IDPSelectorTest {

    @Test
    @DisplayName("should return ANSATTPORTEN when attributes contain eJustice natural person role")
    void chooseIdp_shouldReturnAnsattportenWhenRolePresent() {
        Set<String> attributes = new HashSet<>();
        attributes.add("someOtherAttribute");
        attributes.add(E_JUSTICE_NATURAL_PERSON_ROLE);

        String idp = IDPSelector.chooseIdp(attributes);

        assertEquals(IDPSelector.ANSATTPORTEN, idp);
    }

    @Test
    @DisplayName("should return IDPORTEN when attributes do not contain the role")
    void chooseIdp_shouldReturnIdportenWhenRoleMissing() {
        Set<String> attributes = new HashSet<>();
        attributes.add("someOtherAttribute");

        String idp = IDPSelector.chooseIdp(attributes);

        assertEquals(IDPSelector.IDPORTEN, idp);
    }

    @Test
    @DisplayName("should return IDPORTEN when attributes are null or empty")
    void chooseIdp_shouldReturnIdportenWhenNullOrEmpty() {
        assertEquals(IDPSelector.IDPORTEN, IDPSelector.chooseIdp(null));
        assertEquals(IDPSelector.IDPORTEN, IDPSelector.chooseIdp(Collections.emptySet()));
    }
}
