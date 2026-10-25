package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "daily_task_submissions")
public class DailyTaskSubmissionModel {
    @Id
    private String id;
    private String taskId;          // 任务ID
    /** 学生关联键：账号 Mongo 主键（见 {@link org.example.dto.StudentRef}）。禁止存学号。 */
    private String studentAccountId;
    /** 学号，仅用于展示和导入匹配，禁止用于关联查询。 */
    private String studentId;
    /** 学生姓名，仅用于展示。 */
    private String studentName;
    private String content;         // 提交内容 (可以是JSON字符串或其他格式)
    private String attachments;     // 学生提交的材料 JSON [{url,name}]，与管理员发放文件分开存储
    private LocalDateTime submissionTime;
    private LocalDateTime updateTime;
    private String status;          // 提交状态 (submitted/completed)
}

