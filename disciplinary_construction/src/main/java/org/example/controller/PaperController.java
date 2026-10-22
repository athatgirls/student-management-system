package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.PaperModel;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.service.CurrentUserAccessService;
import org.example.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/msi/papers")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Autowired
    private StudentRepository studentRepository;

    // ==================== 学生端接口 ====================

    @GetMapping("/student/{studentAccountId}")
    public ResponseEntity<?> getStudentPapers(@PathVariable String studentAccountId,
                                              @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        try {
            List<PaperModel> papers = paperService.getPapersByStudentAccountId(studentAccountId);
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取论文数据失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addPaper(@RequestBody PaperModel paper,
                                       @CurrentUser Map<String, Object> currentUser) {
        StudentModel student = currentUserAccessService.requireCurrentStudent(currentUser);
        if (student.getStudentId() == null || student.getStudentId().isBlank())
            throw new AccessDeniedException("当前学生账号缺少学号");
        paper.setStudentAccountId(student.getId());
        paper.setStudentId(student.getStudentId());
        paper.setStudentName(student.getName());
        paper.setScoreRatio(null);
        paper.setPaperScore(null);
        paper.setScorePending(false);
        paper.setPreviousScoreRatio(null);
        paper.setPreviousPaperScore(null);
        try {
            org.example.util.SubmissionValidation.validate(paper);
            resetPaperAudit(paper);
            PaperModel saved = paperService.addPaper(paper);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加论文失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> updatePaper(
            @PathVariable String id,
            @RequestBody PaperModel paper,
            @RequestParam(defaultValue = "false") boolean scoreEdit,
            @CurrentUser Map<String, Object> currentUser) {
        PaperModel existing = paperService.getPaperById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        try {
            paper.setId(id);
            paper.setStudentAccountId(existing.getStudentAccountId());
            paper.setStudentId(existing.getStudentId());
            paper.setStudentName(existing.getStudentName() == null || existing.getStudentName().isBlank()
                    ? currentUserAccessService.requireCurrentStudent(currentUser).getName() : existing.getStudentName());
            org.example.util.SubmissionValidation.validate(paper);
            if (scoreEdit) {
                String error = validateScore(paper.getScoreRatio(), paper.getPaperScore());
                if (error != null) return ResponseEntity.badRequest().body(error);
            }
            boolean changed = scoreEdit && (!Objects.equals(paper.getScoreRatio(), existing.getScoreRatio())
                    || !sameScore(paper.getPaperScore(), existing.getPaperScore()));
            if (changed && !existing.isScorePending()) {
                paper.setPreviousScoreRatio(existing.getScoreRatio());
                paper.setPreviousPaperScore(existing.getPaperScore());
            } else {
                paper.setPreviousScoreRatio(existing.getPreviousScoreRatio());
                paper.setPreviousPaperScore(existing.getPreviousPaperScore());
            }
            paper.setScorePending(existing.isScorePending() || changed);
            if (!scoreEdit) {
                paper.setScoreRatio(existing.getScoreRatio());
                paper.setPaperScore(existing.getPaperScore());
            }
            resetPaperAudit(paper);
            PaperModel updated = paperService.savePaper(paper);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新论文失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<?> deletePaperViaPost(@PathVariable String id,
            @CurrentUser Map<String, Object> currentUser) {
        return deletePaper(id, currentUser);
    }

    public ResponseEntity<?> deletePaper(@PathVariable String id,
                                         @CurrentUser Map<String, Object> currentUser) {
        PaperModel existing = paperService.getPaperById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        try {
            paperService.deletePaper(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除论文失败: " + e.getMessage());
        }
    }

    // 获取单条论文记录
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaper(@PathVariable String id,
                                      @CurrentUser Map<String, Object> currentUser) {
        try {
            PaperModel paper = paperService.getPaperById(id);
            if (paper == null) {
                return ResponseEntity.badRequest().body("论文不存在");
            }
            currentUserAccessService.requireStudentRecord(currentUser, paper.getStudentAccountId());
            return ResponseEntity.ok(paper);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取论文失败: " + e.getMessage());
        }
    }

    // 搜索论文
    @GetMapping("/search")
    public ResponseEntity<?> searchPapers(@RequestParam Map<String, String> params) {
        try {
            List<PaperModel> papers = paperService.searchPapers(params);
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("搜索论文失败: " + e.getMessage());
        }
    }

    // ==================== 管理员端接口 ====================

    @PostMapping("/admin/{id}/score")
    public ResponseEntity<?> updatePaperScore(@PathVariable String id,
            @RequestBody PaperScoreUpdate score, @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) throw new AccessDeniedException("仅管理员可维护论文得分");
        PaperModel paper = paperService.getPaperById(id);
        if (paper == null) return ResponseEntity.notFound().build();
        String error = validateScore(score.getScoreRatio(), score.getPaperScore());
        if (error != null) return ResponseEntity.badRequest().body(error);
        paper.setScoreRatio(score.getScoreRatio());
        paper.setPaperScore(score.getPaperScore());
        clearPendingScore(paper);
        return ResponseEntity.ok(paperService.savePaper(paper));
    }

    public static class PaperScoreUpdate {
        private String scoreRatio;
        private BigDecimal paperScore;
        public String getScoreRatio() { return scoreRatio; }
        public void setScoreRatio(String scoreRatio) { this.scoreRatio = scoreRatio; }
        public BigDecimal getPaperScore() { return paperScore; }
        public void setPaperScore(BigDecimal paperScore) { this.paperScore = paperScore; }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllPapers() {
        try {
            List<PaperModel> papers = paperService.getAllPapers();
            List<String> missingIds = papers.stream()
                    .filter(p -> (p.getStudentName() == null || p.getStudentName().isBlank()) && p.getStudentId() != null)
                    .map(PaperModel::getStudentId).distinct().collect(Collectors.toList());
            if (!missingIds.isEmpty()) {
                Map<String, String> names = studentRepository.findByStudentIdIn(missingIds).stream()
                        .filter(s -> s.getStudentId() != null && s.getName() != null)
                        .collect(Collectors.toMap(StudentModel::getStudentId, StudentModel::getName, (first, second) -> first));
                // Enrich the response only; do not rewrite historical paper records on a GET request.
                papers = papers.stream().map(p -> {
                    if ((p.getStudentName() == null || p.getStudentName().isBlank()) && names.containsKey(p.getStudentId())) {
                        PaperModel copy = new PaperModel();
                        org.springframework.beans.BeanUtils.copyProperties(p, copy);
                        copy.setStudentName(names.get(p.getStudentId()));
                        return copy;
                    }
                    return p;
                }).collect(Collectors.toList());
            }
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取论文数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/admin/{id}/audit")
    public ResponseEntity<?> auditPaper(
            @PathVariable String id, 
            @RequestBody Map<String, String> auditData,
            @CurrentUser Map<String, Object> currentUser) {
        try {
            String auditStatus = auditData.get("auditStatus");
            String auditComment = auditData.get("auditComment");
            String auditorId = currentUserAccessService.requireUserId(currentUser);
            String auditorName = (String) currentUser.get("username");
            
            PaperModel paper = paperService.getPaperById(id);
            if (paper == null) {
                return ResponseEntity.badRequest().body("论文不存在");
            }
            if (paper.isScorePending()) {
                if ("已驳回".equals(auditStatus)) {
                    paper.setScoreRatio(paper.getPreviousScoreRatio());
                    paper.setPaperScore(paper.getPreviousPaperScore());
                }
                if ("已驳回".equals(auditStatus) || "已通过".equals(auditStatus)) clearPendingScore(paper);
            }
            paper.setAuditStatus(auditStatus);
            paper.setAuditComment(auditComment);
            paper.setAuditorId(auditorId);
            paper.setAuditorName(auditorName);
            
            PaperModel updated = paperService.savePaper(paper);
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("审核失败: " + e.getMessage());
        }
    }

    private void resetPaperAudit(PaperModel paper) {
        paper.setAuditStatus("待审核");
        paper.setAuditComment(null);
        paper.setAuditorId(null);
        paper.setAuditorName(null);
    }

    private String validateScore(String ratio, BigDecimal score) {
        if (ratio != null && !Set.of("10:0", "7:3", "6:3:1").contains(ratio)) return "请选择有效的得分比例";
        if (score != null && score.signum() < 0) return "得分不能为负数";
        return null;
    }

    private boolean sameScore(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private void clearPendingScore(PaperModel paper) {
        paper.setScorePending(false);
        paper.setPreviousScoreRatio(null);
        paper.setPreviousPaperScore(null);
    }
}
