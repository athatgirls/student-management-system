package org.example.service;

import org.example.model.CompetitionModel;
import java.util.List;
import java.util.Map;

public interface CompetitionService {
    // 学生操作
    CompetitionModel addCompetition(CompetitionModel competition);
    CompetitionModel updateCompetition(CompetitionModel competition);
    void deleteCompetition(String id);
    /** 参数是学生关联键（账号主键），约定见 {@link org.example.dto.StudentRef}。 */
    List<CompetitionModel> getStudentCompetitions(String studentAccountId);

    // 管理员操作
    List<CompetitionModel> getPendingCompetitions();
    CompetitionModel auditCompetition(String id, String auditStatus, String auditComment, String auditorId);
    List<CompetitionModel> getAllCompetitions();

    // 根据ID获取单个竞赛（Controller需要）
    CompetitionModel getCompetitionById(String id);

    // 新增：Controller 兼容方法（参数是学生关联键：账号主键）
    List<CompetitionModel> getCompetitionsByStudentAccountId(String studentAccountId);
    CompetitionModel saveCompetition(CompetitionModel competition);
    void deleteCompetition(Long id);
    CompetitionModel getCompetitionById(Long id);
    
    // 🌟 学生端需要的方法
    List<CompetitionModel> getActiveCompetitions();
    List<CompetitionModel> searchCompetitions(Map<String, String> params);

    // 统计方法
    int countByAuditStatus(String auditStatus);
}