package org.example.service;

import org.example.model.ActivityModel;
import java.util.List;
import java.util.Map;

public interface ActivityService {
    // 创建活动
    ActivityModel createActivity(ActivityModel activity);
    
    // 获取所有活动
    List<ActivityModel> getAllActivities();
    
    // 根据ID获取活动
    ActivityModel getActivityById(String id);
    
    // 删除活动
    void deleteActivity(String id);
    
    // 导入学生并自动匹配任务
    // students: List<Map<String, String>> 格式：[{"name": "张三", "studentId": "102311306"}, ...]
    Map<String, Object> importStudentsAndMatchTasks(String activityId, List<Map<String, String>> students);
}

