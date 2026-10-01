package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "study_records")
public class StudyRecordModel {
    @Id
    private String id;
    private String studentId;   // 学号
    private String studentDbId; // 数据库ID
    private String semester;    // 学期
    private String course;      // 课程名称
    private Integer score;      // 成绩
    private Double credit;      // 学分
    private String status;      // 状态 (已通过/未通过)
    private String source;      // student: 学生自填；空值为历史教务导入
    private String attachments; // JSON 文件列表
}

