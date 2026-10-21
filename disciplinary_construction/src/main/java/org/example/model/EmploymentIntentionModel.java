package org.example.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Data
@Document("employment_intentions")
public class EmploymentIntentionModel {
    @Id private String id;
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private String intentionType;
    private String targetCity;
    private String targetIndustry;
    private String expectedSalary;
    private String targetPosition;
    private LocalDateTime updateTime;
}
