package org.example.repository;

import org.example.model.PaperModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperRepository extends MongoRepository<PaperModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PaperModel> findByStudentAccountId(String studentAccountId);
    List<PaperModel> findByAuditStatus(String auditStatus);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PaperModel> findByStudentAccountIdAndAuditStatus(String studentAccountId, String auditStatus);

    long countByAuditStatus(String auditStatus);
}