package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.DailyActivityModel;
import org.example.service.DailyActivityService;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msi/daily-activities")
public class DailyActivityController {

    @Autowired
    private DailyActivityService service;
    
    @Autowired
    private StudentRefService studentRefService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody DailyActivityModel model, @CurrentUser Map<String, Object> currentUser) {
        // 学生本人提交填本人身份；管理员代填按学号指定目标学生，姓名不信任请求体
        StudentRef ref = currentUserAccessService.isAdmin(currentUser)
                ? currentUserAccessService.requireAdminTarget(currentUser, model.getStudentId())
                : studentRefService.current(currentUser);
        model.setStudentAccountId(ref.getAccountId());
        model.setStudentId(ref.getStudentId());
        model.setStudentName(ref.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.create(model));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String studentAccountId,
                                                     @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) {
            studentAccountId = currentUserAccessService.requireUserId(currentUser);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        List<DailyActivityModel> activities;
        if (studentAccountId != null && !studentAccountId.isEmpty()) {
            activities = service.findByStudentAccountId(studentAccountId);
        } else {
            activities = service.findAll();
        }
        
        // 填充学号（通过账号主键解析学生信息）
        List<DailyActivityModel> activitiesWithStudentInfo = activities.stream().map(activity -> {
            if (activity.getStudentAccountId() != null && !activity.getStudentAccountId().isEmpty()) {
                try {
                    StudentRef student = studentRefService.resolveByAccountId(activity.getStudentAccountId());
                    if (student != null) {
                        activity.setStudentId(student.getStudentId()); // 学号
                        // studentName 已经在创建时设置了，这里确保正确
                        if (activity.getStudentName() == null || activity.getStudentName().isEmpty()) {
                            activity.setStudentName(student.getName());
                        }
                    }
                } catch (Exception e) {
                    // 忽略错误，保持原有数据
                }
            }
            return activity;
        }).collect(Collectors.toList());
        
        result.put("data", activitiesWithStudentInfo);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/audit")
    public ResponseEntity<Map<String, Object>> audit(@RequestBody Map<String, Object> params, @CurrentUser Map<String, Object> currentUser) {
        String id = (String) params.get("id");
        String status = (String) params.get("auditStatus");
        String comment = (String) params.get("auditComment");
        String auditorId = currentUserAccessService.requireUserId(currentUser);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.audit(id, status, comment, auditorId));
        return ResponseEntity.ok(result);
    }
}
