package com.example.seoanalytics.controller;

import com.example.seoanalytics.dto.ApiResponse;
import com.example.seoanalytics.entity.PlatformAuth;
import com.example.seoanalytics.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url:http://localhost}")
    private String frontendUrl;

    @GetMapping("/gsc/authorize")
    public ApiResponse<Map<String, String>> gscAuthorize(@RequestParam(defaultValue = "1") Long siteId) {
        String url = authService.getGscAuthorizeUrl(siteId);
        return ApiResponse.ok(Map.of("authorizeUrl", url));
    }

    @GetMapping("/gsc/callback")
    public RedirectView gscCallback(@RequestParam String code,
                                    @RequestParam(required = false) String state) {
        Long siteId = state != null ? Long.parseLong(state) : null;
        if (siteId != null) {
            authService.handleGscCallback(code, siteId);
        }
        // OAuth callback hits :8080; redirect back to the Vue SPA on FRONTEND_URL
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return new RedirectView(base + "/settings?gsc=connected");
    }

    @PostMapping("/bing")
    public ApiResponse<PlatformAuth> saveBingKey(@RequestBody Map<String, Object> body) {
        Long siteId = Long.valueOf(body.get("siteId").toString());
        String apiKey = body.get("apiKey").toString();
        return ApiResponse.ok(authService.saveBingApiKey(siteId, apiKey));
    }

    @GetMapping("/status")
    public ApiResponse<List<PlatformAuth>> status(@RequestParam Long siteId) {
        return ApiResponse.ok(authService.listBySite(siteId));
    }
}
