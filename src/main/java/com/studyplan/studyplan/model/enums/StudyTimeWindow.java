package com.studyplan.studyplan.model.enums;

import java.time.LocalTime;

public enum StudyTimeWindow {

    /** Morning block: 06:00 – 12:00 */
    MORNING(LocalTime.of(6, 0)),

    /** Afternoon block: 12:00 – 18:00 */
    AFTERNOON(LocalTime.of(12, 0)),

    /** Evening block: 18:00 – 23:00 */
    EVENING(LocalTime.of(18, 0));

    /** The clock time at which the first session of the day starts. */
    private final LocalTime startTime;

    StudyTimeWindow(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }
}
