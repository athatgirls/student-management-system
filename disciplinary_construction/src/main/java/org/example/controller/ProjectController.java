package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.ProjectModel;
import org.example.service.CurrentUserAccessService;
import org.example.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    // ==================== 学生端接口 ====================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentProjects(@PathVariable String studentId,
                                                @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        try {
            List<ProjectModel> projects = projectService.getProjectsByStudentId(studentId);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取项目数据失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addProject(@RequestBody ProjectModel project,
                                        @CurrentUser Map<String, Object> currentUser) {
        project.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
        project.setStudentName((String) currentUser.get("username"));
        try {
            org.example.util.SubmissionValidation.validate(project);
            resetProjectAudit(project);
            ProjectModel saved = projectService.addProject(project);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加项目失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> updateProject(@PathVariable String id, @RequestBody ProjectModel project,
                                           @CurrentUser Map<String, Object> currentUser) {
        ProjectModel existing = projectService.getProjectById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        try {
            project.setId(id);
            project.setStudentId(existing.getStudentId());
            project.setStudentName(existing.getStudentName());
            org.example.util.SubmissionValidation.validate(project);
            resetProjectAudit(project);
            ProjectModel updated = projectService.saveProject(project);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新项目失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<?> deleteProjectViaPost(@PathVariable String id,
            @CurrentUser Map<String, Object> currentUser) {
        return deleteProject(id, currentUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String id,
                                           @CurrentUser Map<String, Object> currentUser) {
        ProjectModel existing = projectService.getProjectById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除项目失败: " + e.getMessage());
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable String id,
                                        @CurrentUser Map<String, Object> currentUser) {
        try {
            ProjectModel project = projectService.getProjectById(id);
            if (project == null) {
                return ResponseEntity.badRequest().body("项目不存在");
            }
            currentUserAccessService.requireStudentAccess(currentUser, project.getStudentId());
            return ResponseEntity.ok(project);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取项目失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProjects(@RequestParam Map<String, String> params) {
        try {
            List<ProjectModel> projects = projectService.searchProjects(params);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("搜索项目失败: " + e.getMessage());
        }
    }

    // ==================== 管理员端接口 ====================

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllProjects() {
        try {
            List<ProjectModel> projects = projectService.getAllProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取项目数据失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/admin/{id}/audit", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> auditProject(@PathVariable String id, @RequestBody Map<String, String> auditData,
                                          @CurrentUser Map<String, Object> currentUser) {
        try {
            String auditStatus = auditData.get("auditStatus");
            String auditComment = auditData.get("auditComment");
            String auditorId = currentUserAccessService.requireUserId(currentUser);
            String auditorName = (String) currentUser.get("username");

            ProjectModel project = projectService.getProjectById(id);
            if (project == null) {
                return ResponseEntity.badRequest().body("项目不存在");
            }
            
            project.setAuditStatus(auditStatus);
            project.setAuditComment(auditComment);
            project.setAuditorId(auditorId);
            project.setAuditorName(auditorName);

            ProjectModel updated = projectService.saveProject(project);
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("审核失败: " + e.getMessage());
        }
    }

    private void resetProjectAudit(ProjectModel project) {
        project.setAuditStatus("待审核");
        project.setAuditComment(null);
        project.setAuditorId(null);
        project.setAuditorName(null);
    }
}
