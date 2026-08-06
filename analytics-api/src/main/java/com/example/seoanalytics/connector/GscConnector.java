package com.example.seoanalytics.connector;

import com.example.seoanalytics.entity.KeywordDaily;
import com.example.seoanalytics.entity.KeywordPageDaily;
import com.example.seoanalytics.entity.PageDaily;
import com.example.seoanalytics.entity.Platform;
import com.example.seoanalytics.entity.PlatformAuth;
import com.example.seoanalytics.entity.Site;
import com.example.seoanalytics.entity.TrafficDaily;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Search Console connector: OAuth2 + Search Analytics API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GscConnector {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SEARCH_ANALYTICS_URL =
            "https://www.googleapis.com/webmasters/v3/sites/{siteUrl}/searchAnalytics/query";
    private static final String SCOPE = "https://www.googleapis.com/auth/webmasters.readonly";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gsc.client-id:}")
    private String clientId;

    @Value("${gsc.client-secret:}")
    private String clientSecret;

    @Value("${gsc.redirect-uri:}")
    private String redirectUri;

    public String buildAuthorizeUrl(Long siteId) {
        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", siteId)
                .build()
                .toUriString();
    }

    public PlatformAuth exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response = restTemplate.postForEntity(
                TOKEN_URL, new HttpEntity<>(form, headers), String.class);

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            PlatformAuth auth = new PlatformAuth();
            auth.setPlatform(Platform.GSC);
            auth.setAccessToken(node.path("access_token").asText());
            auth.setRefreshToken(node.path("refresh_token").asText(null));
            int expiresIn = node.path("expires_in").asInt(3600);
            auth.setTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            auth.setCreatedAt(LocalDateTime.now());
            return auth;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange GSC OAuth code: " + e.getMessage(), e);
        }
    }

    public String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response = restTemplate.postForEntity(
                TOKEN_URL, new HttpEntity<>(form, headers), String.class);
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return node.path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh GSC token: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> fetchSearchAnalytics(Site site, PlatformAuth auth,
                                                    LocalDate start, LocalDate end) {
        ensureFreshToken(auth);
        String siteUrl = normalizeGscSiteUrl(
                site.getGscProperty() != null && !site.getGscProperty().isBlank()
                        ? site.getGscProperty()
                        : site.getDomain());
        String encodedSite = URLEncoder.encode(siteUrl, StandardCharsets.UTF_8);

        List<TrafficDaily> traffic = queryDimension(auth, encodedSite, start, end, null, site.getId());
        List<KeywordDaily> keywords = queryKeywords(auth, encodedSite, start, end, site.getId());
        List<PageDaily> pages = queryPages(auth, encodedSite, start, end, site.getId());
        List<KeywordPageDaily> keywordPages =
                queryKeywordPages(auth, encodedSite, start, end, site.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("traffic", traffic);
        result.put("keywords", keywords);
        result.put("pages", pages);
        result.put("keywordPages", keywordPages);
        return result;
    }

    private List<TrafficDaily> queryDimension(PlatformAuth auth, String encodedSite,
                                              LocalDate start, LocalDate end,
                                              String dimension, Long siteId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("startDate", start.toString());
        body.put("endDate", end.toString());
        body.put("rowLimit", 25000);
        if (dimension != null) {
            ArrayNode dimensions = body.putArray("dimensions");
            dimensions.add(dimension);
        }

        JsonNode rows = postSearchAnalytics(auth.getAccessToken(), encodedSite, body);
        List<TrafficDaily> list = new ArrayList<>();
        if (rows == null || !rows.isArray()) {
            // Aggregate single period row when no dimension
            TrafficDaily td = new TrafficDaily();
            td.setSiteId(siteId);
            td.setPlatform(Platform.GSC);
            td.setStatDate(end);
            td.setClicks(0L);
            td.setImpressions(0L);
            td.setCtr(0.0);
            td.setAvgPosition(0.0);
            list.add(td);
            return list;
        }
        for (JsonNode row : rows) {
            TrafficDaily td = new TrafficDaily();
            td.setSiteId(siteId);
            td.setPlatform(Platform.GSC);
            if (dimension != null && row.has("keys") && row.get("keys").isArray() && !row.get("keys").isEmpty()) {
                td.setStatDate(LocalDate.parse(row.get("keys").get(0).asText()));
            } else {
                td.setStatDate(end);
            }
            td.setClicks(row.path("clicks").asLong(0));
            td.setImpressions(row.path("impressions").asLong(0));
            td.setCtr(row.path("ctr").asDouble(0));
            td.setAvgPosition(row.path("position").asDouble(0));
            list.add(td);
        }
        return list;
    }

    private List<KeywordDaily> queryKeywords(PlatformAuth auth, String encodedSite,
                                             LocalDate start, LocalDate end, Long siteId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("startDate", start.toString());
        body.put("endDate", end.toString());
        body.put("rowLimit", 25000);
        ArrayNode dimensions = body.putArray("dimensions");
        dimensions.add("date");
        dimensions.add("query");

        JsonNode rows = postSearchAnalytics(auth.getAccessToken(), encodedSite, body);
        List<KeywordDaily> list = new ArrayList<>();
        if (rows == null || !rows.isArray()) {
            return list;
        }
        for (JsonNode row : rows) {
            JsonNode keys = row.path("keys");
            if (!keys.isArray() || keys.size() < 2) {
                continue;
            }
            KeywordDaily kd = new KeywordDaily();
            kd.setSiteId(siteId);
            kd.setPlatform(Platform.GSC);
            kd.setStatDate(LocalDate.parse(keys.get(0).asText()));
            kd.setKeyword(keys.get(1).asText());
            kd.setClicks(row.path("clicks").asLong(0));
            kd.setImpressions(row.path("impressions").asLong(0));
            kd.setAvgPosition(row.path("position").asDouble(0));
            list.add(kd);
        }
        return list;
    }

    private List<PageDaily> queryPages(PlatformAuth auth, String encodedSite,
                                       LocalDate start, LocalDate end, Long siteId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("startDate", start.toString());
        body.put("endDate", end.toString());
        body.put("rowLimit", 25000);
        ArrayNode dimensions = body.putArray("dimensions");
        dimensions.add("date");
        dimensions.add("page");

        JsonNode rows = postSearchAnalytics(auth.getAccessToken(), encodedSite, body);
        List<PageDaily> list = new ArrayList<>();
        if (rows == null || !rows.isArray()) {
            return list;
        }
        for (JsonNode row : rows) {
            JsonNode keys = row.path("keys");
            if (!keys.isArray() || keys.size() < 2) {
                continue;
            }
            PageDaily pd = new PageDaily();
            pd.setSiteId(siteId);
            pd.setPlatform(Platform.GSC);
            pd.setStatDate(LocalDate.parse(keys.get(0).asText()));
            pd.setPageUrl(keys.get(1).asText());
            pd.setClicks(row.path("clicks").asLong(0));
            pd.setImpressions(row.path("impressions").asLong(0));
            list.add(pd);
        }
        return list;
    }

    public List<KeywordPageDaily> queryKeywordPages(
            PlatformAuth auth, String encodedSite, LocalDate start, LocalDate end, Long siteId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("startDate", start.toString());
        body.put("endDate", end.toString());
        body.put("rowLimit", 25000);
        ArrayNode dimensions = body.putArray("dimensions");
        dimensions.add("date");
        dimensions.add("query");
        dimensions.add("page");

        JsonNode rows = postSearchAnalytics(auth.getAccessToken(), encodedSite, body);
        List<KeywordPageDaily> list = new ArrayList<>();
        if (rows == null || !rows.isArray()) {
            return list;
        }
        for (JsonNode row : rows) {
            JsonNode keys = row.path("keys");
            if (!keys.isArray() || keys.size() < 3) {
                continue;
            }
            KeywordPageDaily kp = new KeywordPageDaily();
            kp.setSiteId(siteId);
            kp.setPlatform(Platform.GSC);
            kp.setStatDate(LocalDate.parse(keys.get(0).asText()));
            kp.setKeyword(keys.get(1).asText());
            kp.setPageUrl(keys.get(2).asText());
            kp.setClicks(row.path("clicks").asLong(0));
            kp.setImpressions(row.path("impressions").asLong(0));
            kp.setAvgPosition(row.path("position").asDouble(0));
            list.add(kp);
        }
        return list;
    }

    /**
     * Fetch date-dimensioned traffic for trend charts.
     */
    public List<TrafficDaily> fetchDailyTraffic(Site site, PlatformAuth auth,
                                                LocalDate start, LocalDate end) {
        ensureFreshToken(auth);
        String siteUrl = normalizeGscSiteUrl(
                site.getGscProperty() != null && !site.getGscProperty().isBlank()
                        ? site.getGscProperty()
                        : site.getDomain());
        String encodedSite = URLEncoder.encode(siteUrl, StandardCharsets.UTF_8);
        return queryDimension(auth, encodedSite, start, end, "date", site.getId());
    }

    /** Normalize URL-prefix GSC properties (keep sc-domain: as-is). */
    private String normalizeGscSiteUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("sc-domain:")) {
            return s;
        }
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        return s;
    }

    private JsonNode postSearchAnalytics(String accessToken, String encodedSite, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Build absolute URI with already-encoded siteUrl; URI.create avoids RestTemplate re-encoding
        // which otherwise corrupts https%3A%2F%2F... into http://https%3A%2F%2F...
        String url = "https://www.googleapis.com/webmasters/v3/sites/"
                + encodedSite
                + "/searchAnalytics/query";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    java.net.URI.create(url),
                    HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers),
                    String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("rows");
        } catch (Exception e) {
            log.warn("GSC Search Analytics request failed: {}", e.getMessage());
            return null;
        }
    }

    private void ensureFreshToken(PlatformAuth auth) {
        if (auth.getTokenExpiry() != null && auth.getTokenExpiry().isBefore(LocalDateTime.now().plusMinutes(5))) {
            if (auth.getRefreshToken() != null && !auth.getRefreshToken().isBlank()) {
                String newToken = refreshAccessToken(auth.getRefreshToken());
                auth.setAccessToken(newToken);
                auth.setTokenExpiry(LocalDateTime.now().plusHours(1));
            }
        }
    }
}
