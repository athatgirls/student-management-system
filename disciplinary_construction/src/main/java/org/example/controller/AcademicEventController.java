package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.AcademicEventModel;
import org.example.service.AcademicEventService;
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
@RequestMapping("/msi/academic-events")
public class AcademicEventController {

    @Autowired
    private AcademicEventService service;
    
    @Autowired
    private StudentRefService studentRefService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody AcademicEventModel model, @CurrentUser Map<String, Object> currentUser) {
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
        List<AcademicEventModel> events;
        if (studentAccountId != null && !studentAccountId.isEmpty()) {
            events = service.findByStudentAccountId(studentAccountId);
        } else {
            events = service.findAll();
        }
        
        // 填充学号（通过账号主键解析学生信息）
        List<AcademicEventModel> eventsWithStudentInfo = events.stream().map(event -> {
            if (event.getStudentAccountId() != null && !event.getStudentAccountId().isEmpty()) {
                try {
                    StudentRef student = studentRefService.resolveByAccountId(event.getStudentAccountId());
                    if (student != null) {
                        event.setStudentId(student.getStudentId()); // 学号
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
        String auditorId = currentUserAccessService.requireUserId(currentUser);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.audit(id, status, comment, auditorId));
        return ResponseEntity.ok(result);
    }
}
