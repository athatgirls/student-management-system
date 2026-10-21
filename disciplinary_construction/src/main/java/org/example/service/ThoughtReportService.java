package org.example.service;

import org.example.model.ThoughtReportModel;
import java.util.List;

public interface ThoughtReportService {
    // 学生操作
    ThoughtReportModel addThoughtReport(ThoughtReportModel thoughtReport);
    ThoughtReportModel updateThoughtReport(ThoughtReportModel thoughtReport);
    void deleteThoughtReport(String id);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。 */
    List<ThoughtReportModel> getStudentThoughtReports(String studentAccountId);
    ThoughtReportModel getThoughtReportById(String id);

    // 管理员操作
    List<ThoughtReportModel> getPendingThoughtReports();
    ThoughtReportModel auditThoughtReport(String id, String auditStatus, String auditComment, String auditorId);
    List<ThoughtReportModel> getAllThoughtReports();
    List<ThoughtReportModel> searchThoughtReports(String keyword);
    List<ThoughtReportModel> getThoughtReportsByType(String reportType);
    List<ThoughtReportModel> getThoughtReportsByStatus(String status);
    List<ThoughtReportModel> getThoughtReportsByReviewer(String reviewerId);
} 