package org.example.controller;


import org.example.annotation.CurrentUser;
import org.example.model.CompetitionModel;
import org.example.service.CompetitionService;
import org.example.service.CurrentUserAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/competitions")
public class CompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;


    // ==================== 学生端接口 ====================

    /**
     * 获取学生的竞赛数据
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentCompetitions(@PathVariable String studentId,
                                                     @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        try {
            List<CompetitionModel> competitions = competitionService.getStudentCompetitions(studentId);
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
        competition.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
        competition.setStudentName((String) currentUser.get("username"));
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
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> updateCompetition(@PathVariable String id, @RequestBody CompetitionModel competition,
                                               @CurrentUser Map<String, Object> currentUser) {
        CompetitionModel existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        try {
            competition.setId(id);
            competition.setStudentId(existing.getStudentId());
            competition.setStudentName(existing.getStudentName());
            org.example.util.SubmissionValidation.validate(competition);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompetition(@PathVariable String id,
                                               @CurrentUser Map<String, Object> currentUser) {
        CompetitionModel existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
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
            currentUserAccessService.requireStudentAccess(currentUser, competition.getStudentId());
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
    @RequestMapping(value = "/admin/{id}/audit", method = {RequestMethod.PUT, RequestMethod.POST})
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
}
