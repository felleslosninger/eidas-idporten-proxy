/*
 * Copyright (c) 2023 by European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/page/eupl-text-11-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package no.idporten.eidas.proxy.integration.specificcommunication.service;

import eu.eidas.auth.commons.tx.CorrelationMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Duration;

/**
 * Base implementation of the {@link CorrelationMap} interface based on a ConcurrentMap.
 * <p>
 * Subclass can provide a distributable ConcurrentMap variant (such as Apache Ignite) or an in-memory one
 * using {@link java.util.concurrent.ConcurrentHashMap}.
 *
 * @param <T> the type of the object being stored in the CorrelationMap (e.g. a request).
 * @since 1.1
 */
public abstract class AbstractCorrelationMap<T> implements CorrelationMap<T> {
    private final long lifetimeInSeconds;

    @javax.annotation.Nonnull
    protected RedisCache<String, T> redisCache;

    protected AbstractCorrelationMap(RedisCache<String, T> redisCache, long lifetimeInSeconds) {
        this.redisCache = redisCache;
        this.lifetimeInSeconds = lifetimeInSeconds;
    }

    @javax.annotation.Nullable
    @Override
    public final T get(@javax.annotation.Nonnull String id) {
        return redisCache.get(id);
    }

    @javax.annotation.Nullable
    @Override
    public final T put(@javax.annotation.Nonnull String id, @javax.annotation.Nonnull T value) {
        return redisCache.set(id, value, Duration.ofSeconds(lifetimeInSeconds));
    }

    @Nullable
    @Override
    public final T remove(@Nonnull String id) {
        return redisCache.delete(id);
    }


}
