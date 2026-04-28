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
    private String studentId;       // 学生ID
    private String studentName;     // 学生姓名
    private String content;         // 提交内容 (可以是JSON字符串或其他格式)
    private LocalDateTime submissionTime;
    private LocalDateTime updateTime;
    private String status;          // 提交状态 (submitted/completed)
}

