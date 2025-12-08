package no.idporten.eidas.proxy.service;

import org.springframework.util.CollectionUtils;

import java.util.Set;

import static no.idporten.eidas.proxy.service.EIDAS_ATTRIBUTE_NAMES.E_JUSTICE_NATURAL_PERSON_ROLE;

public class IDPSelector {

    public static final String IDPORTEN = "idporten";
    public static final String ANSATTPORTEN = "ansattporten";

    public static String chooseIdp(Set<String> attributes) {
        if (!CollectionUtils.isEmpty(attributes) && attributes.contains(E_JUSTICE_NATURAL_PERSON_ROLE)) {
            return ANSATTPORTEN;
        }
        return IDPORTEN;
    }
}
