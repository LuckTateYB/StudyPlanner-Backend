package com.studyplan.studyplan.model;
import com.studyplan.studyplan.model.enums.StudyTimeWindow;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    private int hoursPerDay;

    private StudyTimeWindow preferredStudyTime;

}
