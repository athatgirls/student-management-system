package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "thought_reports")
public class ThoughtReportModel {
    @Id
    private String id;
    private String title; // 思想汇报标题
    private String content; // 思想汇报内容
    private String reportDate; // 汇报日期
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private String reportType; // 汇报类型（定期/专项）
    private String status; // 汇报状态
    private String remark; // 备注
    private String reviewerId; // 审核人ID
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String auditStatus; // 审核状态
    private String auditComment; // 审核意见
    private String auditorId; // 审核人ID
    private LocalDateTime auditTime; // 审核时间
} 