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
@Document(collection = "patent")
public class PatentModel {
    @Id
    private String id;

    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private String patentTitle;         // 专利名称
    private String patentType;          // 专利类型
    private String patentNumber;        // 专利号
    private String inventorRank;       // 发明人排名（数字）
    private String coInventors;         // 合作发明人
    private String attachments;         // 附件路径
    private BigDecimal score;
    private boolean scorePending;
    @JsonIgnore
    private BigDecimal previousScore;

    // 专利相关时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate authorizationDate; // 授权时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate applicationDate;   // 受理时间
    private String patentStatus;         // 专利状态：已授权/已受理/申请中

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
