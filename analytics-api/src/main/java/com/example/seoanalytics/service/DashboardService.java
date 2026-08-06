package com.example.seoanalytics.service;

import com.example.seoanalytics.mapper.KeywordDailyMapper;
import com.example.seoanalytics.mapper.KeywordPageDailyMapper;
import com.example.seoanalytics.mapper.PageDailyMapper;
import com.example.seoanalytics.mapper.TrafficDailyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TrafficDailyMapper trafficDailyMapper;
    private final KeywordDailyMapper keywordDailyMapper;
    private final PageDailyMapper pageDailyMapper;
    private final KeywordPageDailyMapper keywordPageDailyMapper;

    public Map<String, Object> getDashboard(Long siteId, String platform, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);

        Map<String, Object> kpi = trafficDailyMapper.sumKpi(siteId, platform, start, end);
        List<Map<String, Object>> trend = trafficDailyMapper.trend(siteId, platform, start, end);
        List<Map<String, Object>> trendByPlatform = trafficDailyMapper.trendByPlatform(siteId, start, end);
        List<Map<String, Object>> topKeywords = keywordDailyMapper.topKeywords(siteId, platform, start, end, 10);

        Map<String, Object> result = new HashMap<>();
        result.put("kpi", normalizeKpi(kpi));
        result.put("trend", trend);
        result.put("trendByPlatform", splitTrendByPlatform(trendByPlatform));
        result.put("topKeywords", topKeywords);
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        return result;
    }

    public Map<String, Object> getSeoData(Long siteId, String platform, LocalDate start, LocalDate end,
                                          int keywordLimit, int pageLimit) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);

        Map<String, Object> result = new HashMap<>();
        result.put("keywords", keywordDailyMapper.topKeywords(siteId, platform, start, end, keywordLimit));
        result.put("pages", pageDailyMapper.topPages(siteId, platform, start, end, pageLimit));
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        return result;
    }

    public List<Map<String, Object>> getKeywordTrend(Long siteId, String keyword, String platform,
                                                     LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);
        return keywordDailyMapper.keywordTrend(siteId, keyword, platform, start, end);
    }

    public Map<String, Object> getPageDetail(Long siteId, String pageUrl, String platform,
                                             LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);
        Map<String, Object> result = new HashMap<>();
        result.put("trend", pageDailyMapper.pageTrend(siteId, pageUrl, platform, start, end));
        result.put("keywords", keywordPageDailyMapper.queryPageMap(siteId, platform, start, end, null, pageUrl, 20));
        return result;
    }

    public List<Map<String, Object>> getQueryPageMap(Long siteId, String platform, LocalDate start, LocalDate end,
                                                     String keyword, String pageUrl, int limit) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);
        return keywordPageDailyMapper.queryPageMap(siteId, platform, start, end, keyword, pageUrl, limit);
    }

    public Map<String, Object> getOpportunities(Long siteId, String platform, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);

        List<Map<String, Object>> keywords = keywordDailyMapper.topKeywords(siteId, platform, start, end, 500);
        List<Map<String, Object>> striking = new ArrayList<>();
        List<Map<String, Object>> lowCtr = new ArrayList<>();

        for (Map<String, Object> row : keywords) {
            String kw = String.valueOf(row.get("keyword"));
            long clicks = toLong(row.get("clicks"));
            long impressions = toLong(row.get("impressions"));
            double position = toDouble(row.get("avg_position"));
            double ctr = impressions > 0 ? (double) clicks / impressions : 0.0;

            if (position >= 4.0 && position <= 20.0 && impressions >= 20) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", "striking_distance");
                item.put("keyword", kw);
                item.put("clicks", clicks);
                item.put("impressions", impressions);
                item.put("ctr", ctr);
                item.put("avgPosition", position);
                item.put("reason", String.format("排名 %.1f（4–20），展现 %d，冲首页潜力", position, impressions));
                striking.add(item);
            }

            if (impressions >= 50) {
                double expected = expectedCtr(position);
                if (ctr < expected * 0.5) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", "low_ctr");
                    item.put("keyword", kw);
                    item.put("clicks", clicks);
                    item.put("impressions", impressions);
                    item.put("ctr", ctr);
                    item.put("avgPosition", position);
                    item.put("reason", String.format(
                            "CTR %.2f%% 低于预期 %.2f%% 的一半（pos %.1f）",
                            ctr * 100, expected * 100, position));
                    lowCtr.add(item);
                }
            }
        }

        striking = striking.stream().limit(50).collect(Collectors.toList());
        lowCtr = lowCtr.stream().limit(50).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strikingDistance", striking);
        result.put("lowCtr", lowCtr);
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());
        return result;
    }

    /** Rough CTR curve by average position for low-CTR detection. */
    private double expectedCtr(double position) {
        if (position <= 1) return 0.30;
        if (position <= 2) return 0.15;
        if (position <= 3) return 0.10;
        if (position <= 4) return 0.07;
        if (position <= 5) return 0.05;
        if (position <= 6) return 0.04;
        if (position <= 7) return 0.03;
        if (position <= 8) return 0.025;
        if (position <= 9) return 0.02;
        if (position <= 10) return 0.02;
        return 0.01;
    }

    public Map<String, Object> comparePlatforms(Long siteId, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);

        List<Map<String, Object>> rows = trafficDailyMapper.kpiByPlatform(siteId, start, end);
        Map<String, Object> gsc = normalizeKpi(null);
        Map<String, Object> bing = normalizeKpi(null);
        for (Map<String, Object> row : rows) {
            String p = String.valueOf(row.get("platform"));
            Map<String, Object> kpi = normalizeKpi(row);
            kpi.put("keywordCount", keywordDailyMapper.countKeywords(siteId, p, start, end));
            if ("GSC".equalsIgnoreCase(p)) gsc = kpi;
            if ("BING".equalsIgnoreCase(p)) bing = kpi;
        }
        if (!gsc.containsKey("keywordCount")) {
            gsc.put("keywordCount", keywordDailyMapper.countKeywords(siteId, "GSC", start, end));
        }
        if (!bing.containsKey("keywordCount")) {
            bing.put("keywordCount", keywordDailyMapper.countKeywords(siteId, "BING", start, end));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("gsc", gsc);
        result.put("bing", bing);
        result.put("trendByPlatform", splitTrendByPlatform(trafficDailyMapper.trendByPlatform(siteId, start, end)));
        return result;
    }

    public Map<String, Object> comparePages(Long siteId, List<String> pageUrls, String platform,
                                            LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);
        List<String> urls = pageUrls == null ? List.of() : pageUrls.stream().limit(5).collect(Collectors.toList());

        List<Map<String, Object>> series = new ArrayList<>();
        List<Map<String, Object>> table = new ArrayList<>();
        for (String url : urls) {
            List<Map<String, Object>> trend = pageDailyMapper.pageTrend(siteId, url, platform, start, end);
            long clicks = trend.stream().mapToLong(r -> toLong(r.get("clicks"))).sum();
            long impressions = trend.stream().mapToLong(r -> toLong(r.get("impressions"))).sum();
            Map<String, Object> s = new HashMap<>();
            s.put("pageUrl", url);
            s.put("trend", trend);
            series.add(s);
            Map<String, Object> row = new HashMap<>();
            row.put("pageUrl", url);
            row.put("clicks", clicks);
            row.put("impressions", impressions);
            row.put("ctr", impressions > 0 ? (double) clicks / impressions : 0);
            table.add(row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("series", series);
        result.put("table", table);
        return result;
    }

    public List<Map<String, Object>> getAlerts(Long siteId, String platform, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(28);
        if (end == null) end = LocalDate.now().minusDays(1);
        LocalDate mid = start.plusDays(Math.max((end.toEpochDay() - start.toEpochDay()) / 2, 1));

        List<Map<String, Object>> recent = keywordDailyMapper.topKeywords(siteId, platform, mid, end, 50);
        List<Map<String, Object>> earlier = keywordDailyMapper.topKeywords(siteId, platform, start, mid.minusDays(1), 50);
        Map<String, Double> earlierPos = new HashMap<>();
        Map<String, Long> earlierClicks = new HashMap<>();
        for (Map<String, Object> row : earlier) {
            String kw = String.valueOf(row.get("keyword"));
            earlierPos.put(kw, toDouble(row.get("avg_position")));
            earlierClicks.put(kw, toLong(row.get("clicks")));
        }

        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Map<String, Object> row : recent) {
            String kw = String.valueOf(row.get("keyword"));
            double pos = toDouble(row.get("avg_position"));
            long clicks = toLong(row.get("clicks"));
            if (earlierPos.containsKey(kw)) {
                double prevPos = earlierPos.get(kw);
                if (prevPos > 0 && pos - prevPos >= 5) {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("type", "position_drop");
                    alert.put("target", kw);
                    alert.put("reason", String.format("排名从 %.1f 下滑至 %.1f", prevPos, pos));
                    alert.put("date", end.toString());
                    alerts.add(alert);
                }
            }
            if (earlierClicks.containsKey(kw)) {
                long prevClicks = earlierClicks.get(kw);
                if (prevClicks >= 10 && clicks < prevClicks * 0.5) {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("type", "traffic_drop");
                    alert.put("target", kw);
                    alert.put("reason", String.format("点击从 %d 下降至 %d", prevClicks, clicks));
                    alert.put("date", end.toString());
                    alerts.add(alert);
                }
            }
            if (alerts.size() >= 30) break;
        }
        return alerts;
    }

    private Map<String, List<Map<String, Object>>> splitTrendByPlatform(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        map.put("GSC", new ArrayList<>());
        map.put("BING", new ArrayList<>());
        if (rows == null) return map;
        for (Map<String, Object> row : rows) {
            String p = String.valueOf(row.get("platform"));
            map.computeIfAbsent(p, k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private Map<String, Object> normalizeKpi(Map<String, Object> kpi) {
        Map<String, Object> normalized = new HashMap<>();
        if (kpi == null) {
            normalized.put("clicks", 0);
            normalized.put("impressions", 0);
            normalized.put("ctr", 0);
            normalized.put("avgPosition", 0);
            return normalized;
        }
        normalized.put("clicks", toNumber(kpi.get("clicks")));
        normalized.put("impressions", toNumber(kpi.get("impressions")));
        normalized.put("ctr", toNumber(kpi.get("ctr")));
        Object avgPos = kpi.get("avg_position");
        if (avgPos == null) avgPos = kpi.get("avgPosition");
        normalized.put("avgPosition", toNumber(avgPos));
        return normalized;
    }

    private Number toNumber(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return (Number) v;
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private long toLong(Object v) {
        return toNumber(v).longValue();
    }

    private double toDouble(Object v) {
        return toNumber(v).doubleValue();
    }
}
