package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Document(collection = "paper")
public class PaperModel {
    @Id
    private String id;

    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private String paperTitle;          // 论文题目
    private String journalName;         // 期刊名称
    private String journalLevel;        // 期刊级别
    private String authorRank;         // 作者排名（数字）
    private String coAuthors;           // 合作作者
    private String scoreRatio;          // 管理员维护的署名得分比例原值
    private BigDecimal paperScore;      // 管理员维护的论文得分
    private boolean scorePending;       // 学生修改的得分尚待管理员审核
    @JsonIgnore
    private String previousScoreRatio;  // 驳回时恢复原得分
    @JsonIgnore
    private BigDecimal previousPaperScore;
    private String doi;                 // DOI号
    private String attachments;         // 附件路径

    // 论文发表相关时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate publicationDate;  // 见刊时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate acceptanceDate;   // 接收时间
    private String publicationStatus;   // 发表状态：已见刊/已接收/审稿中

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
