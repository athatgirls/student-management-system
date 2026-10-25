package org.example.dto;

import lombok.Value;

/**
 * 学生身份值对象 —— 全项目标识约定的唯一权威说明，其他类只引用本文档、不重复粘贴。
 *
 * <p>两种标识，职责互不重叠：
 * <ul>
 *   <li>{@code accountId}：学生账号 Mongo 主键（students._id，即 JWT 中的 userId）。
 *       业务记录的学生关联字段 {@code studentAccountId}、权限校验、附件归属
 *       {@code uploaded_files.ownerId} 只使用它。</li>
 *   <li>{@code studentId}：学号。仅用于页面展示、Excel 导入/导出匹配、管理端指定目标学生。
 *       严禁存入关联字段，严禁用于关联查询。</li>
 * </ul>
 *
 * <p>历史误用（勿重蹈）：拿学号当关联键存库；拿账号主键当学号展示；
 * 用学号与 uploaded_files.ownerId 比较（权限校验会失效）。
 */
@Value
public class StudentRef {
    /** 账号 Mongo 主键（业务关联键 studentAccountId）。 */
    String accountId;
    /** 学号（studentId），仅用于展示/导入匹配。 */
    String studentId;
    /** 姓名，仅用于展示。 */
    String name;
}
