package no.idporten.eidas.proxy.integration.specificcommunication.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.idporten.metric.constants.MetricCategories;
import no.idporten.metric.constants.MetricDescriptions;
import no.idporten.metric.constants.MetricNames;
import no.idporten.metric.constants.MetricValues;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class RedisMetrics {

    private Counter redisTimeoutCounter;

    public RedisMetrics(Optional<MeterRegistry> meterRegistry) {
        meterRegistry.ifPresentOrElse(this::initializeRedisCounter, () -> log.info("RedisMetrics not initialized because MeterRegistry is not present."));

    }

    private void initializeRedisCounter(MeterRegistry meterRegistry) {
        redisTimeoutCounter = Counter
                .builder(MetricNames.APP_EXCEPTION_NAME)
                .tag(MetricCategories.EXTERNAL_SYSTEM, MetricValues.EXTERNAL_SYSTEM_REDIS)
                .tag(MetricCategories.EXCEPTION_TYPE, MetricValues.EXCEPTION_TYPE_TIMEOUT)
                .description(MetricDescriptions.APP_EXCEPTION_REDIS_DESCRIPTION)
                .register(meterRegistry);
        log.info("RedisMetrics initialized");
    }

    public void incrementTimeoutCount() {
        if (redisTimeoutCounter != null) {
            redisTimeoutCounter.increment();
        } else {
            log.warn("Timeout towards redis occurred, but RedisMetrics not initialized. Counter not incremented.");
        }
    }
}
