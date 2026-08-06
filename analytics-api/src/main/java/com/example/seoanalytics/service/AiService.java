package com.example.seoanalytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seoanalytics.entity.AiChat;
import com.example.seoanalytics.mapper.AiChatMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiChatMapper aiChatMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:5001}")
    private String aiServiceUrl;

    public Map<String, Object> chat(Long siteId, String question) {
        Map<String, Object> request = new HashMap<>();
        request.put("question", question);
        request.put("site_id", siteId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String sql = null;
        Object data = null;
        Object ragContext = null;
        String answer;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiServiceUrl + "/chat",
                    new HttpEntity<>(request, headers),
                    String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            sql = root.path("sql").asText(null);
            data = objectMapper.convertValue(root.get("data"), Object.class);
            if (root.has("rag_context") && !root.get("rag_context").isNull()) {
                ragContext = objectMapper.convertValue(root.get("rag_context"), Object.class);
            }
            answer = root.path("answer").asText("No answer returned.");
        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage());
            answer = "AI service unavailable: " + e.getMessage();
        }

        AiChat chat = new AiChat();
        chat.setSiteId(siteId);
        chat.setQuestion(question);
        chat.setSqlGenerated(sql);
        try {
            chat.setDataJson(data != null ? objectMapper.writeValueAsString(data) : null);
        } catch (Exception e) {
            chat.setDataJson(null);
        }
        chat.setAnswer(answer);
        chat.setCreatedAt(LocalDateTime.now());
        aiChatMapper.insert(chat);

        Map<String, Object> result = new HashMap<>();
        result.put("id", chat.getId());
        result.put("sql", sql);
        result.put("data", data);
        result.put("rag_context", ragContext);
        result.put("answer", answer);
        return result;
    }

    public List<AiChat> history(Long siteId, int limit) {
        return aiChatMapper.selectList(
                new LambdaQueryWrapper<AiChat>()
                        .eq(siteId != null, AiChat::getSiteId, siteId)
                        .orderByDesc(AiChat::getCreatedAt)
                        .last("LIMIT " + Math.min(limit, 100)));
    }

    /**
     * Batch-embed keywords via ai-service /embed. Returns number of keywords sent.
     * Failures are logged and return 0 — never throw to callers that must not fail sync.
     */
    public int embedKeywords(Long siteId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }
        List<String> unique = keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .toList();
        if (unique.isEmpty()) {
            return 0;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        int sent = 0;
        final int batchSize = 50;
        for (int i = 0; i < unique.size(); i += batchSize) {
            List<String> batch = unique.subList(i, Math.min(i + batchSize, unique.size()));
            Map<String, Object> body = new HashMap<>();
            body.put("keywords", batch);
            body.put("site_id", siteId);
            try {
                restTemplate.postForEntity(
                        aiServiceUrl + "/embed",
                        new HttpEntity<>(body, headers),
                        String.class);
                sent += batch.size();
            } catch (Exception e) {
                log.warn("Embed batch failed ({} keywords): {}", batch.size(), e.getMessage());
            }
        }
        return sent;
    }
}
