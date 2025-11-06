package com.tbs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RankingRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(RankingRefreshScheduler.class);

    private final RankingService rankingService;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    @Value("${app.ranking.refresh.max-retries:3}")
    private int maxRetries;

    @Value("${app.ranking.refresh.retry-delay-ms:5000}")
    private long retryDelayMs;

    public RankingRefreshScheduler(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @Scheduled(fixedDelay = 300000)
    public void refreshPlayerRankings() {
        if (!isRefreshing.compareAndSet(false, true)) {
            log.warn("Ranking refresh already in progress, skipping scheduled refresh");
            return;
        }

        log.debug("Scheduled refresh of player_rankings materialized view");
        
        int retries = 0;
        boolean success = false;
        
        try {
            while (retries < maxRetries && !success) {
                try {
                    rankingService.refreshPlayerRankings();
                    consecutiveFailures.set(0);
                    success = true;
                    log.debug("Successfully refreshed player_rankings materialized view");
                } catch (Exception e) {
                    retries++;
                    int failures = consecutiveFailures.incrementAndGet();
                    
                    if (retries < maxRetries) {
                        log.warn("Error in scheduled refresh of player_rankings materialized view (attempt {}/{}), retrying in {}ms", 
                                retries, maxRetries, retryDelayMs, e);
                        try {
                            TimeUnit.MILLISECONDS.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Retry sleep interrupted", ie);
                            break;
                        }
                    } else {
                        log.error("Error in scheduled refresh of player_rankings materialized view after {} attempts", 
                                maxRetries, e);
                        
                        if (failures >= 5) {
                            log.error("CRITICAL: {} consecutive failures in ranking refresh scheduler. Manual intervention may be required.", 
                                    failures);
                        }
                    }
                }
            }
        } finally {
            isRefreshing.set(false);
        }
    }
}

