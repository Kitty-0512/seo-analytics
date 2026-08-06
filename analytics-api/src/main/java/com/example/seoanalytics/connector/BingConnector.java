package com.example.seoanalytics.connector;

import com.example.seoanalytics.entity.KeywordDaily;
import com.example.seoanalytics.entity.PageDaily;
import com.example.seoanalytics.entity.Platform;
import com.example.seoanalytics.entity.PlatformAuth;
import com.example.seoanalytics.entity.Site;
import com.example.seoanalytics.entity.TrafficDaily;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bing Webmaster Tools connector: API Key authentication.
 */
@Slf4j
@Component
public class BingConnector {

    private static final String BASE_URL = "https://ssl.bing.com/webmaster/api.svc/json";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bing.api-key:}")
    private String defaultApiKey;

    public Map<String, Object> fetchStats(Site site, PlatformAuth auth, LocalDate start, LocalDate end) {
        String apiKey = resolveApiKey(auth);
        String siteUrl = normalizeBingSiteUrl(
                site.getBingSiteUrl() != null && !site.getBingSiteUrl().isBlank()
                        ? site.getBingSiteUrl()
                        : site.getDomain());

        List<TrafficDaily> traffic = fetchTraffic(apiKey, siteUrl, start, end, site.getId());
        List<KeywordDaily> keywords = fetchKeywordsDaily(apiKey, siteUrl, start, end, site.getId());
        List<PageDaily> pages = fetchPagesDaily(apiKey, siteUrl, start, end, site.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("traffic", traffic);
        result.put("keywords", keywords);
        result.put("pages", pages);
        return result;
    }

    private String normalizeBingSiteUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }

    private List<TrafficDaily> fetchTraffic(String apiKey, String siteUrl,
                                            LocalDate start, LocalDate end, Long siteId) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/GetRankAndTrafficStats")
                .queryParam("siteUrl", siteUrl)
                .queryParam("startDate", start.toString())
                .queryParam("endDate", end.toString())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        List<TrafficDaily> list = new ArrayList<>();
        try {
            JsonNode root = getJson(url);
            JsonNode data = root.path("d");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    TrafficDaily td = new TrafficDaily();
                    td.setSiteId(siteId);
                    td.setPlatform(Platform.BING);
                    String dateStr = item.path("Date").asText(end.toString());
                    // Bing returns /Date(ms)/ format sometimes
                    td.setStatDate(parseBingDate(dateStr, end));
                    td.setClicks(item.path("Clicks").asLong(0));
                    td.setImpressions(item.path("Impressions").asLong(0));
                    long impressions = td.getImpressions();
                    td.setCtr(impressions > 0 ? (double) td.getClicks() / impressions : 0.0);
                    td.setAvgPosition(item.path("AvgImpressionPosition").asDouble(0));
                    list.add(td);
                }
            }
        } catch (Exception e) {
            log.warn("Bing GetRankAndTrafficStats failed: {}", e.getMessage());
        }
        return list;
    }

    private List<KeywordDaily> fetchKeywordsDaily(String apiKey, String siteUrl,
                                                  LocalDate start, LocalDate end, Long siteId) {
        List<KeywordDaily> list = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            list.addAll(fetchKeywordsForDay(apiKey, siteUrl, day, siteId));
        }
        return list;
    }

    private List<PageDaily> fetchPagesDaily(String apiKey, String siteUrl,
                                            LocalDate start, LocalDate end, Long siteId) {
        List<PageDaily> list = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            list.addAll(fetchPagesForDay(apiKey, siteUrl, day, siteId));
        }
        return list;
    }

    private List<KeywordDaily> fetchKeywordsForDay(String apiKey, String siteUrl,
                                                   LocalDate day, Long siteId) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/GetQueryStats")
                .queryParam("siteUrl", siteUrl)
                .queryParam("startDate", day.toString())
                .queryParam("endDate", day.toString())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        List<KeywordDaily> list = new ArrayList<>();
        try {
            JsonNode root = getJson(url);
            JsonNode data = root.path("d");
            if (data.isArray()) {
                int count = 0;
                for (JsonNode item : data) {
                    if (count++ >= 1000) break;
                    KeywordDaily kd = new KeywordDaily();
                    kd.setSiteId(siteId);
                    kd.setPlatform(Platform.BING);
                    kd.setStatDate(day);
                    kd.setKeyword(item.path("Query").asText());
                    kd.setClicks(item.path("Clicks").asLong(0));
                    kd.setImpressions(item.path("Impressions").asLong(0));
                    kd.setAvgPosition(item.path("AvgImpressionPosition").asDouble(0));
                    list.add(kd);
                }
            }
        } catch (Exception e) {
            log.warn("Bing GetQueryStats failed for {}: {}", day, e.getMessage());
        }
        return list;
    }

    private List<PageDaily> fetchPagesForDay(String apiKey, String siteUrl,
                                             LocalDate day, Long siteId) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/GetPageStats")
                .queryParam("siteUrl", siteUrl)
                .queryParam("startDate", day.toString())
                .queryParam("endDate", day.toString())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        List<PageDaily> list = new ArrayList<>();
        try {
            JsonNode root = getJson(url);
            JsonNode data = root.path("d");
            if (data.isArray()) {
                int count = 0;
                for (JsonNode item : data) {
                    if (count++ >= 1000) break;
                    PageDaily pd = new PageDaily();
                    pd.setSiteId(siteId);
                    pd.setPlatform(Platform.BING);
                    pd.setStatDate(day);
                    pd.setPageUrl(item.path("Url").asText());
                    pd.setClicks(item.path("Clicks").asLong(0));
                    pd.setImpressions(item.path("Impressions").asLong(0));
                    list.add(pd);
                }
            }
        } catch (Exception e) {
            log.warn("Bing GetPageStats failed for {}: {}", day, e.getMessage());
        }
        return list;
    }

    private List<KeywordDaily> fetchKeywords(String apiKey, String siteUrl,
                                             LocalDate start, LocalDate end, Long siteId) {
        return fetchKeywordsForDay(apiKey, siteUrl, end, siteId);
    }

    private List<PageDaily> fetchPages(String apiKey, String siteUrl,
                                       LocalDate start, LocalDate end, Long siteId) {
        return fetchPagesForDay(apiKey, siteUrl, end, siteId);
    }

    private JsonNode getJson(String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private String resolveApiKey(PlatformAuth auth) {
        if (auth != null && auth.getApiKey() != null && !auth.getApiKey().isBlank()) {
            return auth.getApiKey();
        }
        return defaultApiKey;
    }

    private LocalDate parseBingDate(String dateStr, LocalDate fallback) {
        try {
            if (dateStr.startsWith("/Date(")) {
                long ms = Long.parseLong(dateStr.replaceAll("[^0-9]", ""));
                return LocalDate.ofEpochDay(ms / 86_400_000L);
            }
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            return fallback;
        }
    }
}
