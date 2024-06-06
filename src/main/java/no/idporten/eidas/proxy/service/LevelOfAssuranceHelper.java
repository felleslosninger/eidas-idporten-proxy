package no.idporten.eidas.proxy.service;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.proxy.config.AcrProperties;
import no.idporten.eidas.proxy.lightprotocol.messages.LevelOfAssurance;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Representation of Level of Assurance
 */
@Component
@RequiredArgsConstructor
public class LevelOfAssuranceHelper {

    private final AcrProperties acrProperties;

    /**
     * @param requestedAcr the originally requested acr levels
     * @param returnedAcr  the acr in the returned token
     * @return true if the match is valid, false if the match is invalid
     */

    public boolean hasValidAcrLevel(String returnedAcr, List<String> requestedAcr) {
        if (requestedAcr == null)
            return isValidAcr(returnedAcr);
        return isValidAcr(returnedAcr) && requestedAcr.stream().anyMatch(a ->
                isEqualOrHigherThan(returnedAcr, a)
        );
    }

    private boolean isEqualOrHigherThan(String candiateAcr, String lowerLimit) {
        return acrProperties.getSupportedAcrValues().indexOf(lowerLimit) <= acrProperties.getSupportedAcrValues().indexOf(candiateAcr);
    }


    protected boolean isValidAcr(String returnedAcr) {
        return acrProperties.getSupportedAcrValues().contains(returnedAcr);
    }

    protected List<String> eidasAcrListToIdportenAcrList(List<ILevelOfAssurance> acrLevels) {
        return acrLevels.stream()
                .map(ILevelOfAssurance::getValue)
                .map(acrProperties.getAcrValueMapToIdporten()::get)
                .toList();
    }

    public ILevelOfAssurance idportenAcrListToEidasAcr(String idportenAcrLevel) {
        return LevelOfAssurance.fromString(acrProperties.getAcrValueMapFromIdporten().get(idportenAcrLevel));
    }
}