package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.PatentModel;
import org.example.service.CurrentUserAccessService;
import org.example.service.PatentService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/msi/patents")
public class PatentController {

    @Autowired
    private PatentService patentService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Autowired
    private StudentRefService studentRefService;

    // ==================== 学生端接口 ====================

    @GetMapping("/student/{studentAccountId}")
    public ResponseEntity<?> getStudentPatents(@PathVariable String studentAccountId,
                                               @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        try {
            List<PatentModel> patents = patentService.getPatentsByStudentAccountId(studentAccountId);
            return ResponseEntity.ok(patents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取专利数据失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addPatent(@RequestBody PatentModel patent,
                                       @CurrentUser Map<String, Object> currentUser) {
        StudentRef ref = studentRefService.current(currentUser);
        if (ref.getStudentId() == null || ref.getStudentId().isBlank())
            throw new AccessDeniedException("当前学生账号缺少学号");
        patent.setStudentAccountId(ref.getAccountId());
        patent.setStudentId(ref.getStudentId());
        patent.setStudentName(ref.getName());
        patent.setScore(null);
        patent.setScorePending(false);
        patent.setPreviousScore(null);
        try {
            org.example.util.SubmissionValidation.validate(patent);
            resetPatentAudit(patent);
            PatentModel saved = patentService.addPatent(patent);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加专利失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> updatePatent(@PathVariable String id, @RequestBody PatentModel patent,
                                           @RequestParam(defaultValue = "false") boolean scoreEdit,
                                           @CurrentUser Map<String, Object> currentUser) {
        PatentModel existing = patentService.getPatentById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        try {
            patent.setId(id);
            patent.setStudentAccountId(existing.getStudentAccountId());
            patent.setStudentId(existing.getStudentId());
            patent.setStudentName(existing.getStudentName());
            org.example.util.SubmissionValidation.validate(patent);
            if (scoreEdit && patent.getScore() != null && patent.getScore().signum() < 0)
                return ResponseEntity.badRequest().body("得分不能为负数");
            boolean changed = scoreEdit && !sameScore(patent.getScore(), existing.getScore());
            patent.setPreviousScore(changed && !existing.isScorePending() ? existing.getScore() : existing.getPreviousScore());
            patent.setScorePending(existing.isScorePending() || changed);
            if (!scoreEdit) patent.setScore(existing.getScore());
            resetPatentAudit(patent);
            PatentModel updated = patentService.savePatent(patent);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新专利失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<?> deletePatentViaPost(@PathVariable String id,
            @CurrentUser Map<String, Object> currentUser) {
        return deletePatent(id, currentUser);
    }

    public ResponseEntity<?> deletePatent(@PathVariable String id,
                                          @CurrentUser Map<String, Object> currentUser) {
        PatentModel existing = patentService.getPatentById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
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
            currentUserAccessService.requireStudentRecord(currentUser, patent.getStudentAccountId());
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

    @PostMapping("/admin/{id}/score")
    public ResponseEntity<?> updatePatentScore(@PathVariable String id, @RequestBody ScoreUpdate input,
                                                @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) throw new AccessDeniedException("仅管理员可维护专利得分");
        PatentModel patent = patentService.getPatentById(id);
        if (patent == null) return ResponseEntity.notFound().build();
        if (input.getScore() != null && input.getScore().signum() < 0) return ResponseEntity.badRequest().body("得分不能为负数");
        patent.setScore(input.getScore());
        patent.setScorePending(false);
        patent.setPreviousScore(null);
        return ResponseEntity.ok(patentService.savePatent(patent));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllPatents() {
        try {
            List<PatentModel> patents = patentService.getAllPatents();
            return ResponseEntity.ok(patents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取专利数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/admin/{id}/audit")
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
            if (patent.isScorePending() && ("已驳回".equals(auditStatus) || "已通过".equals(auditStatus))) {
                if ("已驳回".equals(auditStatus)) patent.setScore(patent.getPreviousScore());
                patent.setScorePending(false);
                patent.setPreviousScore(null);
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

    private boolean sameScore(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    public static class ScoreUpdate {
        private BigDecimal score;
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
    }
}
