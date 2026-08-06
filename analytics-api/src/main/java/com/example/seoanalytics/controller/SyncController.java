package com.example.seoanalytics.controller;

import com.example.seoanalytics.dto.ApiResponse;
import com.example.seoanalytics.entity.Platform;
import com.example.seoanalytics.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping
    public ApiResponse<Map<String, Object>> sync(
            @RequestParam Long siteId,
            @RequestParam(required = false) Platform platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (platform == null) {
                return ApiResponse.ok(syncService.syncAll(siteId, startDate, endDate));
            }
            return ApiResponse.ok(syncService.sync(siteId, platform, startDate, endDate));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam Long siteId) {
        return ApiResponse.ok(syncService.getSyncStatus(siteId));
    }
}
