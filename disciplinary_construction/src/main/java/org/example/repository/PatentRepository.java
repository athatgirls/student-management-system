package org.example.repository;

import org.example.model.PatentModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatentRepository extends MongoRepository<PatentModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PatentModel> findByStudentAccountId(String studentAccountId);
    List<PatentModel> findByAuditStatus(String auditStatus);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PatentModel> findByStudentAccountIdAndAuditStatus(String studentAccountId, String auditStatus);

    long countByAuditStatus(String auditStatus);
}