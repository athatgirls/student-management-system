package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.PaperModel;
import org.example.service.CurrentUserAccessService;
import org.example.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/papers")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    // ==================== 学生端接口 ====================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentPapers(@PathVariable String studentId,
                                              @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        try {
            List<PaperModel> papers = paperService.getPapersByStudentId(studentId);
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取论文数据失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addPaper(@RequestBody PaperModel paper,
                                      @CurrentUser Map<String, Object> currentUser) {
        paper.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
        paper.setStudentName((String) currentUser.get("username"));
        try {
            resetPaperAudit(paper);
            PaperModel saved = paperService.savePaper(paper);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加论文失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePaper(
            @PathVariable String id,
            @RequestBody PaperModel paper,
            @CurrentUser Map<String, Object> currentUser) {
        PaperModel existing = paperService.getPaperById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        try {
            paper.setId(id);
            paper.setStudentId(existing.getStudentId());
            paper.setStudentName(existing.getStudentName());
            resetPaperAudit(paper);
            PaperModel updated = paperService.savePaper(paper);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新论文失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable String id,
                                         @CurrentUser Map<String, Object> currentUser) {
        PaperModel existing = paperService.getPaperById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
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
            currentUserAccessService.requireStudentAccess(currentUser, paper.getStudentId());
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

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllPapers() {
        try {
            List<PaperModel> papers = paperService.getAllPapers();
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取论文数据失败: " + e.getMessage());
        }
    }

    @PutMapping("/admin/{id}/audit")
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
}
