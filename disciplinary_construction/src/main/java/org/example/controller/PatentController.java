package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.PatentModel;
import org.example.service.CurrentUserAccessService;
import org.example.service.PatentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/patents")
public class PatentController {

    @Autowired
    private PatentService patentService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    // ==================== 学生端接口 ====================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentPatents(@PathVariable String studentId,
                                               @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        try {
            List<PatentModel> patents = patentService.getPatentsByStudentId(studentId);
            return ResponseEntity.ok(patents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取专利数据失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addPatent(@RequestBody PatentModel patent,
                                       @CurrentUser Map<String, Object> currentUser) {
        patent.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
        patent.setStudentName((String) currentUser.get("username"));
        try {
            resetPatentAudit(patent);
            PatentModel saved = patentService.savePatent(patent);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加专利失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePatent(@PathVariable String id, @RequestBody PatentModel patent,
                                          @CurrentUser Map<String, Object> currentUser) {
        PatentModel existing = patentService.getPatentById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        try {
            patent.setId(id);
            patent.setStudentId(existing.getStudentId());
            patent.setStudentName(existing.getStudentName());
            resetPatentAudit(patent);
            PatentModel updated = patentService.savePatent(patent);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新专利失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePatent(@PathVariable String id,
                                          @CurrentUser Map<String, Object> currentUser) {
        PatentModel existing = patentService.getPatentById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        try {
            patentService.deletePatent(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除专利失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPatent(@PathVariable String id,
                                       @CurrentUser Map<String, Object> currentUser) {
        try {
            PatentModel patent = patentService.getPatentById(id);
            if (patent == null) {
                return ResponseEntity.badRequest().body("专利不存在");
            }
            currentUserAccessService.requireStudentAccess(currentUser, patent.getStudentId());
            return ResponseEntity.ok(patent);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取专利失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPatents(@RequestParam Map<String, String> params) {
        try {
            List<PatentModel> patents = patentService.searchPatents(params);
            return ResponseEntity.ok(patents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("搜索专利失败: " + e.getMessage());
        }
    }

    // ==================== 管理员端接口 ====================

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllPatents() {
        try {
            List<PatentModel> patents = patentService.getAllPatents();
            return ResponseEntity.ok(patents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取专利数据失败: " + e.getMessage());
        }
    }

    @PutMapping("/admin/{id}/audit")
    public ResponseEntity<?> auditPatent(@PathVariable String id, @RequestBody Map<String, String> auditData,
                                         @CurrentUser Map<String, Object> currentUser) {
        try {
            String auditStatus = auditData.get("auditStatus");
            String auditComment = auditData.get("auditComment");
            String auditorId = currentUserAccessService.requireUserId(currentUser);
            String auditorName = (String) currentUser.get("username");

            PatentModel patent = patentService.getPatentById(id);
            if (patent == null) {
                return ResponseEntity.badRequest().body("专利不存在");
            }
            
            patent.setAuditStatus(auditStatus);
            patent.setAuditComment(auditComment);
            patent.setAuditorId(auditorId);
            patent.setAuditorName(auditorName);

            PatentModel updated = patentService.savePatent(patent);
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("审核失败: " + e.getMessage());
        }
    }

    private void resetPatentAudit(PatentModel patent) {
        patent.setAuditStatus("待审核");
        patent.setAuditComment(null);
        patent.setAuditorId(null);
        patent.setAuditorName(null);
    }
}
