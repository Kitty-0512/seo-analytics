package com.example.seoanalytics.controller;

import com.example.seoanalytics.dto.ApiResponse;
import com.example.seoanalytics.entity.AiChat;
import com.example.seoanalytics.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String question = body.get("question") != null ? body.get("question").toString() : null;
        if (question == null || question.isBlank()) {
            return ApiResponse.fail("question is required");
        }
        Long siteId = body.get("siteId") != null ? Long.valueOf(body.get("siteId").toString()) : null;
        return ApiResponse.ok(aiService.chat(siteId, question));
    }

    @GetMapping("/history")
    public ApiResponse<List<AiChat>> history(
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(aiService.history(siteId, limit));
    }
}
