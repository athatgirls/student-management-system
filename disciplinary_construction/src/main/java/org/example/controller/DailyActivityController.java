package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.DailyActivityModel;
import org.example.model.StudentModel;
import org.example.service.DailyActivityService;
import org.example.service.StudentService;
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
    private StudentService studentService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody DailyActivityModel model, @CurrentUser Map<String, Object> currentUser) {
        String userId = (String) currentUser.get("userId");
        String username = (String) currentUser.get("username");
        model.setStudentId(userId);
        model.setStudentName(username);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.create(model));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String studentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        List<DailyActivityModel> activities;
        if (studentId != null && !studentId.isEmpty()) {
            activities = service.findByStudentId(studentId);
        } else {
            activities = service.findAll();
        }
        
        // 填充学号（通过studentId查询学生信息）
        List<DailyActivityModel> activitiesWithStudentInfo = activities.stream().map(activity -> {
            if (activity.getStudentId() != null && !activity.getStudentId().isEmpty()) {
                try {
                    StudentModel student = studentService.findById(activity.getStudentId());
                    if (student != null) {
                        activity.setStudentNumber(student.getStudentId()); // 学号
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
        String auditorId = (String) currentUser.get("userId");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.audit(id, status, comment, auditorId));
        return ResponseEntity.ok(result);
    }
}

