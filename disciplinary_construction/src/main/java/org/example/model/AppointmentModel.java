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
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
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
