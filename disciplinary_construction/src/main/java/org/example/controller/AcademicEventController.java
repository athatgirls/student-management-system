package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.AcademicEventModel;
import org.example.model.StudentModel;
import org.example.service.AcademicEventService;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msi/academic-events")
public class AcademicEventController {

    @Autowired
    private AcademicEventService service;
    
    @Autowired
    private StudentService studentService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody AcademicEventModel model, @CurrentUser Map<String, Object> currentUser) {
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
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String studentId,
                                                     @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) {
            studentId = currentUserAccessService.requireUserId(currentUser);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        List<AcademicEventModel> events;
        if (studentId != null && !studentId.isEmpty()) {
            events = service.findByStudentId(studentId);
        } else {
            events = service.findAll();
        }
        
        // 填充学号（通过studentId查询学生信息）
        List<AcademicEventModel> eventsWithStudentInfo = events.stream().map(event -> {
            if (event.getStudentId() != null && !event.getStudentId().isEmpty()) {
                try {
                    StudentModel student = studentService.findById(event.getStudentId());
                    if (student != null) {
                        event.setStudentNumber(student.getStudentId()); // 学号
                        // studentName 已经在创建时设置了，这里确保正确
                        if (event.getStudentName() == null || event.getStudentName().isEmpty()) {
                            event.setStudentName(student.getName());
                        }
                    }
                } catch (Exception e) {
                    // 忽略错误，保持原有数据
                }
            }
            return event;
        }).collect(Collectors.toList());
        
        result.put("data", eventsWithStudentInfo);
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
