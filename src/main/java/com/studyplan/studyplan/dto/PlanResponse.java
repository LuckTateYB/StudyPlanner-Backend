package com.studyplan.studyplan.dto;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {

    /** The complete list of daily study plans, sorted chronologically. */
    private List<DayPlan> studyPlan;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayPlan {

        /** The calendar date for this day's study block. */
        private LocalDate date;

        /** Ordered list of sessions for this day. */
        private List<SessionInfo> sessions;
    }

    /** Compact representation of one study session. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionInfo {

        /** Name of the course to study. */
        private String course;

        /** Clock time the session starts (e.g., 18:00). */
        private LocalTime startTime;

        /** Duration of the session in hours (always 1 in current implementation). */
        private int duration;
    }

}
