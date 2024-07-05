/* 
#   Copyright (c) 2017 European Commission  
#   Licensed under the EUPL, Version 1.2 or – as soon they will be 
#   approved by the European Commission - subsequent versions of the 
#    EUPL (the "Licence"); 
#    You may not use this work except in compliance with the Licence. 
#    You may obtain a copy of the Licence at: 
#    * https://joinup.ec.europa.eu/page/eupl-text-11-12  
#    *
#    Unless required by applicable law or agreed to in writing, software 
#    distributed under the Licence is distributed on an "AS IS" basis, 
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
#    See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package no.idporten.eidas.proxy.integration.specificcommunication.caches;


import no.idporten.eidas.proxy.integration.specificcommunication.service.AbstractCorrelationMap;
import no.idporten.eidas.proxy.integration.specificcommunication.service.RedisCache;


/**
 * Default implementation of the CorrelationMap for specific {@link CorrelatedRequestHolder} instances.
 *
 * @since 2.0
 */
public final class OIDCRequestCache extends AbstractCorrelationMap<CorrelatedRequestHolder> {

    public OIDCRequestCache(final RedisCache<String, CorrelatedRequestHolder> redisCache, final long oidcRequestStateLifetimeSeconds) {
        super(redisCache, oidcRequestStateLifetimeSeconds);
    }
}
