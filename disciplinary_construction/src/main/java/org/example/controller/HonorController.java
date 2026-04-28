package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.HonorModel;
import org.example.model.StudentModel;
import org.example.service.HonorService;
import org.example.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msi/honors")
public class HonorController {

    @Autowired
    private HonorService service;
    
    @Autowired
    private StudentService studentService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody HonorModel model, @CurrentUser Map<String, Object> currentUser) {
        String userId = (String) currentUser.get("userId");
        model.setUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.create(model));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        List<HonorModel> honors;
        if (userId != null && !userId.isEmpty()) {
            honors = service.findByUserId(userId);
        } else {
            honors = service.findAll();
        }
        
        // 填充学生姓名和学号
        List<HonorModel> honorsWithStudentInfo = honors.stream().map(honor -> {
            if (honor.getUserId() != null && !honor.getUserId().isEmpty()) {
                try {
                    StudentModel student = studentService.findById(honor.getUserId());
                    if (student != null) {
                        honor.setStudentName(student.getName());
                        honor.setStudentId(student.getStudentId());
                    }
                } catch (Exception e) {
                    // 忽略错误，保持原有数据
                }
            }
            return honor;
        }).collect(Collectors.toList());
        
        result.put("data", honorsWithStudentInfo);
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

