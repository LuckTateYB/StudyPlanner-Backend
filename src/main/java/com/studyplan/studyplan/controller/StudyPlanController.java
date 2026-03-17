package com.studyplan.studyplan.controller;

import com.studyplan.studyplan.dto.PlanRequest;
import com.studyplan.studyplan.dto.PlanResponse;
import com.studyplan.studyplan.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @PostMapping("/generate")
    public ResponseEntity<PlanResponse> generatePlan(@Valid @RequestBody PlanRequest request) {
        log.info("POST /plan/generate received — courses: {}, preferredTime: {}",
                request.getCourses().size(), request.getPreferredStudyTime());

        PlanResponse response = studyPlanService.generatePlan(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Study Planner API is running ✓");
    }
}
