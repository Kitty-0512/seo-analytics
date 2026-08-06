package com.example.seoanalytics.service;

import com.example.seoanalytics.connector.BingConnector;
import com.example.seoanalytics.connector.GscConnector;
import com.example.seoanalytics.entity.KeywordDaily;
import com.example.seoanalytics.entity.KeywordPageDaily;
import com.example.seoanalytics.entity.PageDaily;
import com.example.seoanalytics.entity.Platform;
import com.example.seoanalytics.entity.PlatformAuth;
import com.example.seoanalytics.entity.Site;
import com.example.seoanalytics.entity.TrafficDaily;
import com.example.seoanalytics.mapper.KeywordDailyMapper;
import com.example.seoanalytics.mapper.KeywordPageDailyMapper;
import com.example.seoanalytics.mapper.PageDailyMapper;
import com.example.seoanalytics.mapper.TrafficDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SiteService siteService;
    private final AuthService authService;
    private final AiService aiService;
    private final GscConnector gscConnector;
    private final BingConnector bingConnector;
    private final TrafficDailyMapper trafficDailyMapper;
    private final KeywordDailyMapper keywordDailyMapper;
    private final PageDailyMapper pageDailyMapper;
    private final KeywordPageDailyMapper keywordPageDailyMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public Map<String, Object> sync(Long siteId, Platform platform, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);

        Site site = siteService.getById(siteId);
        if (site == null) {
            throw new IllegalArgumentException("Site not found: " + siteId);
        }

        PlatformAuth auth = authService.getAuth(siteId, platform);
        if (auth == null) {
            throw new IllegalStateException("No auth configured for platform " + platform + " on site " + siteId);
        }

        Map<String, Object> fetched;
        if (platform == Platform.GSC) {
            List<TrafficDaily> dailyTraffic = gscConnector.fetchDailyTraffic(site, auth, start, end);
            Map<String, Object> aggregates = gscConnector.fetchSearchAnalytics(site, auth, start, end);
            @SuppressWarnings("unchecked")
            List<KeywordDaily> keywords = (List<KeywordDaily>) aggregates.get("keywords");
            @SuppressWarnings("unchecked")
            List<PageDaily> pages = (List<PageDaily>) aggregates.get("pages");
            @SuppressWarnings("unchecked")
            List<KeywordPageDaily> keywordPages = (List<KeywordPageDaily>) aggregates.get("keywordPages");
            fetched = new HashMap<>();
            fetched.put("traffic", dailyTraffic);
            fetched.put("keywords", keywords);
            fetched.put("pages", pages);
            fetched.put("keywordPages", keywordPages);
            authService.persistAuth(auth);
        } else {
            fetched = bingConnector.fetchStats(site, auth, start, end);
        }

        @SuppressWarnings("unchecked")
        List<TrafficDaily> trafficList = (List<TrafficDaily>) fetched.get("traffic");
        @SuppressWarnings("unchecked")
        List<KeywordDaily> keywordList = (List<KeywordDaily>) fetched.get("keywords");
        @SuppressWarnings("unchecked")
        List<PageDaily> pageList = (List<PageDaily>) fetched.get("pages");
        @SuppressWarnings("unchecked")
        List<KeywordPageDaily> keywordPageList = (List<KeywordPageDaily>) fetched.get("keywordPages");

        int trafficCount = 0, keywordCount = 0, pageCount = 0, keywordPageCount = 0;
        Set<String> distinctKeywords = new LinkedHashSet<>();
        if (trafficList != null) {
            for (TrafficDaily row : trafficList) {
                trafficDailyMapper.upsert(row);
                trafficCount++;
            }
        }
        if (keywordList != null) {
            for (KeywordDaily row : keywordList) {
                keywordDailyMapper.upsert(row);
                if (row.getKeyword() != null && !row.getKeyword().isBlank()) {
                    distinctKeywords.add(row.getKeyword());
                }
                keywordCount++;
            }
        }
        if (pageList != null) {
            for (PageDaily row : pageList) {
                pageDailyMapper.upsert(row);
                pageCount++;
            }
        }
        if (keywordPageList != null) {
            for (KeywordPageDaily row : keywordPageList) {
                keywordPageDailyMapper.upsert(row);
                keywordPageCount++;
            }
        }

        String cacheKey = "sync:last:" + siteId + ":" + platform;
        String nowIso = LocalDateTime.now().toString();
        redisTemplate.opsForValue().set(cacheKey, nowIso, 30, TimeUnit.DAYS);

        int embedded = 0;
        if (!distinctKeywords.isEmpty()) {
            try {
                embedded = aiService.embedKeywords(siteId, new ArrayList<>(distinctKeywords));
            } catch (Exception e) {
                log.warn("Post-sync embed failed for site {} {}: {}", siteId, platform, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("siteId", siteId);
        result.put("platform", platform);
        result.put("startDate", start);
        result.put("endDate", end);
        result.put("trafficRows", trafficCount);
        result.put("keywordRows", keywordCount);
        result.put("pageRows", pageCount);
        result.put("keywordPageRows", keywordPageCount);
        result.put("embeddedKeywords", embedded);
        result.put("lastSyncAt", nowIso);
        log.info("Sync completed for site {} platform {}: traffic={}, keywords={}, pages={}, keywordPages={}, embed={}",
                siteId, platform, trafficCount, keywordCount, pageCount, keywordPageCount, embedded);
        return result;
    }

    @Transactional
    public Map<String, Object> syncAll(Long siteId, LocalDate start, LocalDate end) {
        Map<String, Object> result = new HashMap<>();
        for (Platform platform : Platform.values()) {
            PlatformAuth auth = authService.getAuth(siteId, platform);
            if (auth != null) {
                try {
                    result.put(platform.getValue(), sync(siteId, platform, start, end));
                } catch (Exception e) {
                    log.error("Sync failed for {}: {}", platform, e.getMessage());
                    result.put(platform.getValue(), Map.of("error", e.getMessage()));
                }
            }
        }
        return result;
    }

    public Map<String, Object> getSyncStatus(Long siteId) {
        Map<String, Object> platforms = new LinkedHashMap<>();
        String latest = null;
        for (Platform platform : Platform.values()) {
            Object val = redisTemplate.opsForValue().get("sync:last:" + siteId + ":" + platform);
            if (val != null) {
                String s = val.toString();
                platforms.put(platform.getValue(), s);
                if (latest == null || s.compareTo(latest) > 0) {
                    latest = s;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("siteId", siteId);
        result.put("lastSyncAt", latest);
        result.put("platforms", platforms);
        return result;
    }
}
