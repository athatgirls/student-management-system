package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Document(collection = "project")
public class ProjectModel {
    @Id
    private String id;

    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private String projectName;         // 项目名称
    private String projectLevel;        // 项目级别：校（市）级、省（部）级、国家级
    private String ranking;            // 排名（数字）
    private String teamMembers;         // 团队成员
    private String instructorName;      // 指导老师
    private String attachments;         // 附件路径
    private BigDecimal score;
    private boolean scorePending;
    @JsonIgnore
    private BigDecimal previousScore;

    // 项目起止时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate startDate;        // 项目开始时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate endDate;          // 项目结束时间

    // 审核相关
    private String auditStatus;         // 审核状态
    private String auditComment;        // 审核意见
    private String auditorId;           // 审核人ID
    private String auditorName;         // 审核人姓名
    private LocalDateTime auditTime;    // 审核时间

    // 时间戳
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime updateTime;   // 更新时间
}
