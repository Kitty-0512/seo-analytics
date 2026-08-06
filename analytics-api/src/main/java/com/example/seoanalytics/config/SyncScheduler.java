package com.example.seoanalytics.config;

import com.example.seoanalytics.entity.Site;
import com.example.seoanalytics.service.SiteService;
import com.example.seoanalytics.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SiteService siteService;
    private final SyncService syncService;

    @Value("${app.sync.enabled:true}")
    private boolean enabled;

    @Value("${app.sync.lookback-days:3}")
    private int lookbackDays;

    @Scheduled(cron = "${app.sync.cron:0 0 2 * * *}")
    public void scheduledSync() {
        if (!enabled) {
            return;
        }
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(Math.max(lookbackDays - 1, 0));
        List<Site> sites = siteService.listAll();
        log.info("Scheduled sync starting for {} sites, range {} → {}", sites.size(), start, end);
        for (Site site : sites) {
            try {
                syncService.syncAll(site.getId(), start, end);
            } catch (Exception e) {
                log.error("Scheduled sync failed for site {}: {}", site.getId(), e.getMessage());
            }
        }
        log.info("Scheduled sync finished");
    }
}
