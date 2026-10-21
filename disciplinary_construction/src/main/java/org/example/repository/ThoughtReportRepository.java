package org.example.repository;

import org.example.model.ThoughtReportModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThoughtReportRepository extends MongoRepository<ThoughtReportModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<ThoughtReportModel> findByStudentAccountId(String studentAccountId);
    List<ThoughtReportModel> findByAuditStatus(String auditStatus);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<ThoughtReportModel> findByStudentAccountIdAndAuditStatus(String studentAccountId, String auditStatus);
    List<ThoughtReportModel> findByReportType(String reportType);
    List<ThoughtReportModel> findByStatus(String status);
    List<ThoughtReportModel> findByTitleContaining(String title);
    List<ThoughtReportModel> findByStudentNameContaining(String studentName);
    List<ThoughtReportModel> findByReviewerId(String reviewerId);
} 