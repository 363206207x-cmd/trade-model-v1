package org.example.trademodel.analysisrun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AnalysisRunClockConfig {
    @Bean
    public Clock analysisRunClock() {
        return Clock.systemUTC();
    }
}
