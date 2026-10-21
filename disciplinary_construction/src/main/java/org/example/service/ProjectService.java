package org.example.service;

import org.example.model.ProjectModel;
import java.util.List;
import java.util.Map;

public interface ProjectService {
    // 学生操作
    ProjectModel addProject(ProjectModel project);
    ProjectModel updateProject(ProjectModel project);
    void deleteProject(String id);
    /** 参数是学生关联键（账号主键），约定见 {@link org.example.dto.StudentRef}。 */
    List<ProjectModel> getStudentProjects(String studentAccountId);

    // 管理员操作
    List<ProjectModel> getPendingProjects();
    ProjectModel auditProject(String id, String auditStatus, String auditComment, String auditorId);
    List<ProjectModel> getAllProjects();

    // 兼容方法（参数是学生关联键：账号主键）
    List<ProjectModel> getProjectsByStudentAccountId(String studentAccountId);
    ProjectModel saveProject(ProjectModel project);
    void deleteProject(Long id);
    ProjectModel getProjectById(Long id);
    ProjectModel getProjectById(String id);
    
    // 搜索方法
    List<ProjectModel> searchProjects(Map<String, String> params);

    // 统计方法
    int countByAuditStatus(String auditStatus);
}