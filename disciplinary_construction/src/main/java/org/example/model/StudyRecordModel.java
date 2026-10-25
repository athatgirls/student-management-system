package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "study_records")
public class StudyRecordModel {
    @Id
    private String id;
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    private String semester;    // 学期
    private String course;      // 课程名称
    private Integer score;      // 成绩
    private Double credit;      // 学分
    private String courseType;  // degree: 学位课；non_degree: 非学位课
    private String status;      // 状态 (已通过/未通过)
    private String source;      // student: 学生自填；空值为历史教务导入
    private String attachments; // JSON 文件列表
}
