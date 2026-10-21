package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "appointments")
public class AppointmentModel {
    @Id private String id;
    private String studentDbId;
    private String studentId;
    private String studentName;
    private String position;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String attachments;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
