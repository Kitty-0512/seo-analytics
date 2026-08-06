package com.example.seoanalytics.controller;

import com.example.seoanalytics.dto.ApiResponse;
import com.example.seoanalytics.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(
            @RequestParam Long siteId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(dashboardService.getDashboard(siteId, platform, startDate, endDate));
    }

    @GetMapping("/seo")
    public ApiResponse<Map<String, Object>> seo(
            @RequestParam Long siteId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "50") int keywordLimit,
            @RequestParam(defaultValue = "50") int pageLimit) {
        return ApiResponse.ok(dashboardService.getSeoData(siteId, platform, startDate, endDate, keywordLimit, pageLimit));
    }

    @GetMapping("/seo/keyword-trend")
    public ApiResponse<List<Map<String, Object>>> keywordTrend(
            @RequestParam Long siteId,
            @RequestParam String keyword,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(dashboardService.getKeywordTrend(siteId, keyword, platform, startDate, endDate));
    }

    @GetMapping("/seo/page-detail")
    public ApiResponse<Map<String, Object>> pageDetail(
            @RequestParam Long siteId,
            @RequestParam String pageUrl,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(dashboardService.getPageDetail(siteId, pageUrl, platform, startDate, endDate));
    }

    @GetMapping("/seo/query-page")
    public ApiResponse<List<Map<String, Object>>> queryPage(
            @RequestParam Long siteId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String pageUrl,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(dashboardService.getQueryPageMap(
                siteId, platform, startDate, endDate, keyword, pageUrl, limit));
    }

    @GetMapping("/seo/opportunities")
    public ApiResponse<Map<String, Object>> opportunities(
            @RequestParam Long siteId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(dashboardService.getOpportunities(siteId, platform, startDate, endDate));
    }

    @GetMapping("/compare/platform")
    public ApiResponse<Map<String, Object>> comparePlatform(
            @RequestParam Long siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(dashboardService.comparePlatforms(siteId, startDate, endDate));
    }

    @GetMapping("/compare/pages")
    public ApiResponse<Map<String, Object>> comparePages(
            @RequestParam Long siteId,
            @RequestParam String pageUrls,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<String> urls = Arrays.stream(pageUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return ApiResponse.ok(dashboardService.comparePages(siteId, urls, platform, startDate, endDate));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<Map<String, Object>>> alerts(
            @RequestParam Long siteId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(dashboardService.getAlerts(siteId, platform, startDate, endDate));
    }
}
