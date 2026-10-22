package org.example.controller;


import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.CompetitionModel;
import org.example.service.CompetitionService;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/msi/competitions")
public class CompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Autowired
    private StudentRefService studentRefService;


    // ==================== 学生端接口 ====================

    /**
     * 获取学生的竞赛数据
     */
    @GetMapping("/student/{studentAccountId}")
    public ResponseEntity<?> getStudentCompetitions(@PathVariable String studentAccountId,
                                                     @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        try {
            List<CompetitionModel> competitions = competitionService.getStudentCompetitions(studentAccountId);
            return ResponseEntity.ok(competitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取竞赛数据失败: " + e.getMessage());
        }
    }

    /**
     * 添加竞赛
     */
    @PostMapping
    public ResponseEntity<?> addCompetition(@RequestBody CompetitionModel competition,
                                            @CurrentUser Map<String, Object> currentUser) {
        StudentRef ref = studentRefService.current(currentUser);
        if (ref.getStudentId() == null || ref.getStudentId().isBlank())
            throw new AccessDeniedException("当前学生账号缺少学号");
        competition.setStudentAccountId(ref.getAccountId());
        competition.setStudentId(ref.getStudentId());
        competition.setStudentName(ref.getName());
        competition.setScore(null);
        competition.setScorePending(false);
        competition.setPreviousScore(null);
        try {
            org.example.util.SubmissionValidation.validate(competition);
            resetCompetitionAudit(competition);
            CompetitionModel saved = competitionService.addCompetition(competition);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加竞赛失败: " + e.getMessage());
        }
    }

    /**
     * 更新竞赛
     */
    @PostMapping("/{id}")
    public ResponseEntity<?> updateCompetition(@PathVariable String id, @RequestBody CompetitionModel competition,
                                                @RequestParam(defaultValue = "false") boolean scoreEdit,
                                                @CurrentUser Map<String, Object> currentUser) {
        CompetitionModel existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        try {
            competition.setId(id);
            competition.setStudentAccountId(existing.getStudentAccountId());
            competition.setStudentId(existing.getStudentId());
            competition.setStudentName(existing.getStudentName());
            org.example.util.SubmissionValidation.validate(competition);
            if (scoreEdit && competition.getScore() != null && competition.getScore().signum() < 0)
                return ResponseEntity.badRequest().body("得分不能为负数");
            boolean changed = scoreEdit && !sameScore(competition.getScore(), existing.getScore());
            competition.setPreviousScore(changed && !existing.isScorePending() ? existing.getScore() : existing.getPreviousScore());
            competition.setScorePending(existing.isScorePending() || changed);
            if (!scoreEdit) competition.setScore(existing.getScore());
            resetCompetitionAudit(competition);
            CompetitionModel updated = competitionService.updateCompetition(competition);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新竞赛失败: " + e.getMessage());
        }
    }

    /**
     * 删除竞赛
     */
    @PostMapping("/{id}/delete")
    public ResponseEntity<?> deleteCompetitionViaPost(@PathVariable String id,
            @CurrentUser Map<String, Object> currentUser) {
        return deleteCompetition(id, currentUser);
    }

    public ResponseEntity<?> deleteCompetition(@PathVariable String id,
                                               @CurrentUser Map<String, Object> currentUser) {
        CompetitionModel existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        try {
            competitionService.deleteCompetition(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除竞赛失败: " + e.getMessage());
        }
    }


    // 获取单条竞赛记录
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompetition(@PathVariable String id,
                                            @CurrentUser Map<String, Object> currentUser) {
        try {
            CompetitionModel competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return ResponseEntity.badRequest().body("竞赛不存在");
            }
            currentUserAccessService.requireStudentRecord(currentUser, competition.getStudentAccountId());
            return ResponseEntity.ok(competition);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取竞赛失败: " + e.getMessage());
        }
    }

    // 获取活跃的竞赛列表（用于下拉选择）
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCompetitions() {
        try {
            List<CompetitionModel> competitions = competitionService.getActiveCompetitions();
            return ResponseEntity.ok(competitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取活跃竞赛失败: " + e.getMessage());
        }
    }

    // 新增：搜索竞赛
    @GetMapping("/search")
    public ResponseEntity<?> searchCompetitions(@RequestParam Map<String, String> params) {
        try {
            List<CompetitionModel> competitions = competitionService.searchCompetitions(params);
            return ResponseEntity.ok(competitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("搜索竞赛失败: " + e.getMessage());
        }
    }

    // ==================== 管理员端接口 ====================

    @PostMapping("/admin/{id}/score")
    public ResponseEntity<?> updateCompetitionScore(@PathVariable String id, @RequestBody ScoreUpdate input,
                                                     @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) throw new AccessDeniedException("仅管理员可维护竞赛得分");
        CompetitionModel competition = competitionService.getCompetitionById(id);
        if (competition == null) return ResponseEntity.notFound().build();
        if (input.getScore() != null && input.getScore().signum() < 0) return ResponseEntity.badRequest().body("得分不能为负数");
        competition.setScore(input.getScore());
        competition.setScorePending(false);
        competition.setPreviousScore(null);
        return ResponseEntity.ok(competitionService.saveCompetition(competition));
    }

    /**
     * 获取所有竞赛数据（管理员）
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllCompetitions() {
        try {
            List<CompetitionModel> competitions = competitionService.getAllCompetitions();
            return ResponseEntity.ok(competitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取竞赛数据失败: " + e.getMessage());
        }
    }

    /**
     * 审核竞赛
     */
    @PostMapping("/admin/{id}/audit")
    public ResponseEntity<?> auditCompetition(@PathVariable String id, @RequestBody Map<String, String> auditData,
                                              @CurrentUser Map<String, Object> currentUser) {
        try {
            String auditStatus = auditData.get("auditStatus");
            String auditComment = auditData.get("auditComment");
            String auditorId = currentUserAccessService.requireUserId(currentUser);
            String auditorName = (String) currentUser.get("username");
            
            CompetitionModel competition = competitionService.getCompetitionById(id);
            if (competition == null) {
                return ResponseEntity.badRequest().body("竞赛不存在");
            }
            if (competition.isScorePending() && ("已驳回".equals(auditStatus) || "已通过".equals(auditStatus))) {
                if ("已驳回".equals(auditStatus)) competition.setScore(competition.getPreviousScore());
                competition.setScorePending(false);
                competition.setPreviousScore(null);
            }
            
            competition.setAuditStatus(auditStatus);
            competition.setAuditComment(auditComment);
            competition.setAuditorId(auditorId);
            competition.setAuditorName(auditorName);

            CompetitionModel updated = competitionService.saveCompetition(competition);
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("审核失败: " + e.getMessage());
        }
    }

    private void resetCompetitionAudit(CompetitionModel competition) {
        competition.setAuditStatus("待审核");
        competition.setAuditComment(null);
        competition.setAuditorId(null);
        competition.setAuditorName(null);
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
