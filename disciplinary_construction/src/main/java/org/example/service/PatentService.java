package org.example.service;

import org.example.model.PatentModel;
import java.util.List;
import java.util.Map;

public interface PatentService {
    // 学生操作
    PatentModel addPatent(PatentModel patent);
    PatentModel updatePatent(PatentModel patent);
    void deletePatent(String id);
    /** 参数是学生关联键（账号主键），约定见 {@link org.example.dto.StudentRef}。 */
    List<PatentModel> getStudentPatents(String studentAccountId);

    // 管理员操作
    List<PatentModel> getPendingPatents();
    PatentModel auditPatent(String id, String auditStatus, String auditComment, String auditorId);
    List<PatentModel> getAllPatents();
    
    PatentModel getPatentById(String id); 

    // 兼容方法（参数是学生关联键：账号主键）
    List<PatentModel> getPatentsByStudentAccountId(String studentAccountId);
    PatentModel savePatent(PatentModel patent);
    void deletePatent(Long id);
    PatentModel getPatentById(Long id);

    // 搜索方法
    List<PatentModel> searchPatents(Map<String, String> params);

    // 统计方法
    int countByAuditStatus(String auditStatus);
}