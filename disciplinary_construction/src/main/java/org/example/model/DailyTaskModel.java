package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "daily_tasks")
public class DailyTaskModel {
    @Id
    private String id;
    private String title;           // 任务标题
    private String description;     // 任务描述
    private String type;            // 任务类型 (如: info_filling)
    private LocalDateTime deadline; // 截止日期
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String creatorId;       // 创建者ID (管理员)
    private boolean active = true;  // 是否激活
    private List<TaskField> fields; // 任务字段定义（类似Excel表头）
    private String taskCategory;    // 任务类别：normal(普通任务，所有人都需要完成) 或 registration(报名型任务，限制人数)
    private Integer maxParticipants; // 报名型任务的最大报名人数（仅报名型任务有效）
    private Integer currentParticipants; // 当前已报名人数（仅报名型任务有效，自动计算）
    
    // 范围限制
    private List<String> allowedGrades; // 允许的年级列表（如：["2023", "2024"]），为空表示不限制
    private List<String> allowedIdentities; // 接收政治面貌（如：入党积极分子、发展对象），为空表示全部政治面貌
    private List<String> allowedPoliticalStatuses; // 允许的政治面貌列表（如：["党员", "预备党员"]），为空表示不限制
    private List<String> allowedPartyStages; // 允许的入党阶段列表（如：["入党积极分子", "发展对象"]），为空表示不限制
    
    // 内部类：字段定义
    @Data
    public static class TaskField {
        private String fieldName;    // 字段名称（如：姓名、学号）
        private String fieldType;     // 字段类型（text, number, date, select等）
        private boolean required;     // 是否必填
        private String placeholder;   // 占位符提示
        private List<String> options; // 如果是select类型，选项列表
        private String conditionalField; // 条件字段：依赖的字段名（当依赖字段的值等于conditionalValue时才显示）
        private String conditionalValue; // 条件值：当依赖字段等于此值时才显示当前字段
    }
}
