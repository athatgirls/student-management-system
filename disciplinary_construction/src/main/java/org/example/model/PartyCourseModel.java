package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "party_courses")
public class PartyCourseModel {
    @Id
    private String id;
    private String title; // 课程标题
    private String description; // 课程描述
    private String courseDate; // 课程日期
    private String courseType; // 课程类型
    private String instructor; // 授课教师
    private String location; // 上课地点
    private String duration; // 课程时长
    private String materials; // 课程材料
    private String participants; // 参与人员
    private String status; // 课程状态
    private String remark; // 备注
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String auditStatus; // 审核状态
    private String auditComment; // 审核意见
    private String auditorId; // 审核人ID
    private LocalDateTime auditTime; // 审核时间
} 