package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "party_applications")
public class PartyApplicationModel {
    @Id
    private String id;
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    private String name; // 姓名
    private String currentStage; // 当前阶段：提交申请书/入党积极分子/发展对象/预备党员/正式党员
    private String applicationDate; // 提交入党申请书日期
    private String branch; // 所属党支部
    
    // 已提交的材料列表
    private List<SubmittedMaterial> submittedMaterials;
    
    // 材料类
    @Data
    public static class SubmittedMaterial {
        private String materialType; // 材料类型：入党申请书/思想汇报/个人自传/政审材料/转正申请书等
        private String materialName; // 材料名称
        private String submitDate; // 提交日期
        private String fileUrl; // 文件URL
        private String status; // 审核状态：待审核/已通过/已拒绝
        private String auditComment; // 审核意见
    }
    
    private String remark; // 备注
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String auditStatus; // 整体审核状态
    private String auditComment; // 审核意见
    private String auditorId; // 审核人ID
    private LocalDateTime auditTime; // 审核时间
}

