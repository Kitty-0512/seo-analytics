package com.example.seoanalytics.controller;

import com.example.seoanalytics.dto.ApiResponse;
import com.example.seoanalytics.entity.Site;
import com.example.seoanalytics.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public ApiResponse<List<Site>> list() {
        return ApiResponse.ok(siteService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Site> get(@PathVariable Long id) {
        Site site = siteService.getById(id);
        if (site == null) {
            return ApiResponse.fail("Site not found");
        }
        return ApiResponse.ok(site);
    }

    @PostMapping
    public ApiResponse<Site> create(@RequestBody Site site) {
        if (site.getName() == null || site.getDomain() == null) {
            return ApiResponse.fail("name and domain are required");
        }
        return ApiResponse.ok(siteService.create(site));
    }

    @PutMapping("/{id}")
    public ApiResponse<Site> update(@PathVariable Long id, @RequestBody Site site) {
        try {
            return ApiResponse.ok(siteService.update(id, site));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true));
    }
}
