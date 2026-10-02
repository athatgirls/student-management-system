package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.HonorModel;
import org.example.model.StudentModel;
import org.example.service.HonorService;
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
@RequestMapping("/msi/honors")
public class HonorController {

    @Autowired
    private HonorService service;
    
    @Autowired
    private StudentService studentService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody HonorModel model, @CurrentUser Map<String, Object> currentUser) {
        String userId = currentUserAccessService.requireCurrentStudent(currentUser).getId();
        if (model.getEvidenceUrl() == null || model.getEvidenceUrl().isBlank())
            return ResponseEntity.badRequest().body(Map.of("code", 400, "msg", "请上传荣誉证明材料"));
        model.setUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.create(model));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String userId,
                                                      @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) {
            userId = currentUserAccessService.requireUserId(currentUser);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        List<HonorModel> honors;
        if (userId != null && !userId.isEmpty()) {
            honors = service.findByUserId(userId);
        } else {
            honors = service.findAll();
        }
        
        // 填充学生姓名和学号
        List<HonorModel> honorsWithStudentInfo = honors.stream()
                .map(this::withStudentInfo)
                .collect(Collectors.toList());
        
        result.put("data", honorsWithStudentInfo);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String id,
                                                       @CurrentUser Map<String, Object> currentUser) {
        HonorModel honor = service.findById(id);
        if (honor == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, honor.getUserId());
        return ResponseEntity.ok(Map.of("code", 200, "data", withStudentInfo(honor)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id,
                                                       @RequestBody HonorModel model,
                                                       @CurrentUser Map<String, Object> currentUser) {
        HonorModel existing = service.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getUserId());
        if (model.getEvidenceUrl() == null || model.getEvidenceUrl().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "msg", "请上传荣誉证明材料"));
        }
        model.setId(id);
        model.setUserId(existing.getUserId());
        model.setCreateTime(existing.getCreateTime());
        return ResponseEntity.ok(Map.of("code", 200, "data", service.update(model)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id,
                                                       @CurrentUser Map<String, Object> currentUser) {
        HonorModel existing = service.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getUserId());
        service.delete(id);
        return ResponseEntity.ok(Map.of("code", 200));
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

    private HonorModel withStudentInfo(HonorModel honor) {
        if (studentService == null || honor.getUserId() == null || honor.getUserId().isEmpty()) {
            return honor;
        }
        try {
            StudentModel student = studentService.findById(honor.getUserId());
            if (student != null) {
                honor.setStudentName(student.getName());
                honor.setStudentId(student.getStudentId());
            }
        } catch (Exception e) {
            // Keep the honor record available when a student record cannot be loaded.
        }
        return honor;
    }
}
