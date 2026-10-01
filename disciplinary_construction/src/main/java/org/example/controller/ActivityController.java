package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.ActivityModel;
import org.example.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    // 创建活动
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createActivity(@RequestBody ActivityModel activity, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String creatorId = (String) currentUser.get("userId");
            activity.setCreatorId(creatorId);
            ActivityModel savedActivity = activityService.createActivity(activity);
            result.put("code", 200);
            result.put("data", savedActivity);
            result.put("msg", "活动创建成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "活动创建失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取所有活动
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllActivities() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ActivityModel> activities = activityService.getAllActivities();
            result.put("code", 200);
            result.put("data", activities);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取活动列表失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 根据ID获取活动
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getActivityById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ActivityModel activity = activityService.getActivityById(id);
            if (activity != null) {
                result.put("code", 200);
                result.put("data", activity);
            } else {
                result.put("code", 404);
                result.put("msg", "活动不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取活动失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 删除活动
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> deleteActivity(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            activityService.deleteActivity(id);
            result.put("code", 200);
            result.put("msg", "活动删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "活动删除失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 导入学生并自动匹配任务
    @PostMapping("/{activityId}/import-students")
    public ResponseEntity<Map<String, Object>> importStudentsAndMatchTasks(
            @PathVariable String activityId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> students = (List<Map<String, String>>) request.get("students");
            if (students == null || students.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "学生列表不能为空");
                return ResponseEntity.ok(result);
            }

            Map<String, Object> matchResult = activityService.importStudentsAndMatchTasks(activityId, students);
            result.put("code", 200);
            result.put("data", matchResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "导入学生失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}

