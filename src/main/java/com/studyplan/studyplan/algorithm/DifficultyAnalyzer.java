package com.studyplan.studyplan.algorithm;

import com.studyplan.studyplan.config.AiClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifficultyAnalyzer {

    private final RestTemplate aiRestTemplate;
    private final AiClientConfig aiConfig;

    public Map<String, Integer> analyzeDifficulty(List<String> courseNames) {
        log.info("Requesting AI difficulty analysis for courses: {}", courseNames);
        try {
            String prompt = buildPrompt(courseNames);
            String rawResponse = callAiApi(prompt);
            Map<String, Integer> difficulties = parseResponse(rawResponse, courseNames);
            log.info("AI difficulty results: {}", difficulties);
            return difficulties;
        } catch (Exception e) {
            log.warn("AI difficulty analysis failed ({}). Using fallback difficulty 3 for all courses.", e.getMessage());
            return buildFallbackDifficulties(courseNames);
        }
    }

    private String buildPrompt(List<String> courseNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an academic advisor. Rate the difficulty of each course below on a scale from 1 to 5:\n");
        sb.append("  1 = Very easy\n  2 = Easy\n  3 = Medium\n  4 = Hard\n  5 = Very hard\n\n");
        sb.append("Courses:\n");
        for (String name : courseNames) {
            sb.append("- ").append(name).append("\n");
        }
        sb.append("\nRespond ONLY with lines in the format:\n");
        sb.append("CourseName - DifficultyNumber\n\n");
        sb.append("Example:\nMathematics - 5\nHistory - 2\n\nDo not include any additional text.");
        return sb.toString();
    }

    /**
     * Calls the AI REST API with the given prompt using the Anthropic Messages format.
     *
     * @param prompt the user prompt text
     * @return the raw text content from the AI response
     */
    @SuppressWarnings("unchecked")
    private String callAiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getAiApiKey());
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "study-planner");
        // Build the Anthropic Messages API request body
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiConfig.getAiModel());
        requestBody.put("max_tokens", aiConfig.getMaxTokens());
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = aiRestTemplate.exchange(
                aiConfig.getAiApiUrl(),
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Extract text from the Anthropic response: content[0].text
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty response body from AI API");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("No choices in AI API response");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private Map<String, Integer> parseResponse(String rawResponse, List<String> courseNames) {
        Map<String, Integer> difficulties = new LinkedHashMap<>();
        // Initialize all courses with fallback difficulty 3
        for (String name : courseNames) {
            difficulties.put(name, 3);
        }
        // Pattern: "CourseName - 4"  (allows multi-word names)
        Pattern pattern = Pattern.compile("^(.+?)\\s*-\\s*(\\d)\\s*$");
        for (String line : rawResponse.split("\\n")) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.matches()) {
                String parsedName  = matcher.group(1).trim();
                int    parsedLevel = Integer.parseInt(matcher.group(2));
                int    clamped     = Math.max(1, Math.min(5, parsedLevel)); // clamp to [1, 5]

                // Match against known course names (case-insensitive)
                for (String name : courseNames) {
                    if (name.equalsIgnoreCase(parsedName)) {
                        difficulties.put(name, clamped);
                        break;
                    }
                }
            }
        }
        return difficulties;
    }

    /**
     * Builds a fallback difficulty map assigning level 3 to every course.
     *
     * @param courseNames the list of course names
     * @return map with all difficulties set to 3
     */
    private Map<String, Integer> buildFallbackDifficulties(List<String> courseNames) {
        Map<String, Integer> fallback = new LinkedHashMap<>();
        for (String name : courseNames) {
            fallback.put(name, 3);
        }
        return fallback;
    }
}
