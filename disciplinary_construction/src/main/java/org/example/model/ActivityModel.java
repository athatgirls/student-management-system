package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "activities")
public class ActivityModel {
    @Id
    private String id;
    private String title;           // 活动标题
    private LocalDateTime activityTime; // 活动时间
    private String location;        // 活动地点
    private String creatorId;       // 创建者ID (管理员)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 关联的任务ID（只匹配这个特定任务）
    private String taskId;
    
    // 参与学生列表（存储学生的MongoDB _id）
    private List<String> participantStudentIds;
    
    // 是否已自动匹配到任务（用于标记是否已经处理过）
    private boolean matched = false;
}

