package org.example.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Data
@Document("employment_intentions")
public class EmploymentIntentionModel {
    @Id private String id;
    private String studentId, studentName, intentionType, targetCity, targetIndustry, expectedSalary, targetPosition;
    private LocalDateTime updateTime;
}
