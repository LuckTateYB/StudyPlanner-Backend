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
        sb.append("You are an experienced academic advisor and curriculum designer.\n");
        sb.append("Rate the academic difficulty of each course below on a scale from 1 to 5,\n");
        sb.append("considering ALL of the following dimensions:\n\n");
        sb.append("  - Conceptual difficulty: How abstract or complex are the core ideas?\n");
        sb.append("  - Mathematical complexity: Does it require significant quantitative reasoning?\n");
        sb.append("  - Memorization load: How much content must be memorized vs. understood?\n");
        sb.append("  - Problem-solving demand: Does it require applying knowledge to novel problems?\n\n");
        sb.append("Difficulty scale:\n");
        sb.append("  1 = Very easy   (minimal effort, mostly review)\n");
        sb.append("  2 = Easy        (straightforward, light workload)\n");
        sb.append("  3 = Medium      (moderate effort, some challenging concepts)\n");
        sb.append("  4 = Hard        (high workload, abstract reasoning required)\n");
        sb.append("  5 = Very hard   (extremely demanding, significant time investment)\n\n");
        sb.append("Courses to rate:\n");
        for (String name : courseNames) {
            sb.append("- ").append(name).append("\n");
        }
        sb.append("\nRules:\n");
        sb.append("- Respond ONLY with one line per course, in this exact format:\n");
        sb.append("  CourseName - DifficultyNumber\n");
        sb.append("- Do NOT include explanations, headers, or any other text.\n");
        sb.append("- Use the EXACT course name as provided.\n\n");
        sb.append("Example output:\n");
        sb.append("Calculus II - 5\n");
        sb.append("World History - 2\n");
        sb.append("Intro to Programming - 3\n");
        return sb.toString();
    }

    /**
     * Llama a la API REST de IA con el mensaje proporcionado
     */
    @SuppressWarnings("unchecked")
    private String callAiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getAiApiKey());
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "study-planner");
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
        for (String name : courseNames) {
            difficulties.put(name, 3); // fallback por defecto
        }

        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("AI returned empty response; using fallback difficulties.");
            return difficulties;
        }

        // Patrón tolerante: permite espacios adicionales, texto final opcional y dígitos del 1 al 5.
        // Coincide con: "CourseName - 4" o "CourseName - 4 (Hard)" o "CourseName: 4"
        Pattern pattern = Pattern.compile(
                "^(.+?)\\s*[-:]\\s*([1-5])(?:\\s.*)?$",
                Pattern.CASE_INSENSITIVE
        );

        for (String line : rawResponse.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            Matcher matcher = pattern.matcher(trimmed);
            if (!matcher.matches()) {
                log.debug("Skipping unrecognized line from AI response: '{}'", trimmed);
                continue;
            }

            String parsedName  = matcher.group(1).trim();
            int    parsedLevel = Integer.parseInt(matcher.group(2));
            int    clamped     = Math.max(1, Math.min(5, parsedLevel));

            // Exact match first, then case-insensitive, then partial/fuzzy
            boolean matched = false;
            for (String name : courseNames) {
                if (name.equalsIgnoreCase(parsedName)) {
                    difficulties.put(name, clamped);
                    matched = true;
                    break;
                }
            }
            // Fuzzy fallback: check if one contains the other (handles minor AI rephrasing)
            if (!matched) {
                for (String name : courseNames) {
                    if (name.toLowerCase().contains(parsedName.toLowerCase())
                            || parsedName.toLowerCase().contains(name.toLowerCase())) {
                        log.debug("Fuzzy-matched AI response '{}' to course '{}'", parsedName, name);
                        difficulties.put(name, clamped);
                        break;
                    }
                }
            }
        }
        return difficulties;
    }

    /**
     * Crea un mapa de dificultad de reserva asignando el nivel 3 a cada curso.
     * @param courseNames lista nombre de cursos
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
