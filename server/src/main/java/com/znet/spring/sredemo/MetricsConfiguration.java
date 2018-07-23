package com.znet.spring.sredemo;

import java.time.Duration;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * Metrics configuration class used to setup the Micrometer registry
 * and configure how it sends its distribution of metrics.
 */
@Configuration
public class MetricsConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCustomization() {
    	Duration STEP = Duration.ofSeconds(5);
    	Duration HISTOGRAM_EXPIRY = Duration.ofMinutes(10);

        return registry -> registry.config()
        	.meterFilter(new MeterFilter() {
                @Override
                public DistributionStatisticConfig configure(Meter.Id id,
                                                             DistributionStatisticConfig config) {
                    
                	// ensure the percentiles are included in the metrics
                    return config.merge(DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .percentiles(0.5, 0.75, 0.95)
                            .expiry(HISTOGRAM_EXPIRY)
                            .bufferLength((int) (HISTOGRAM_EXPIRY.toMillis() / STEP.toMillis()))
                            .build());
                }
            });
    }

}