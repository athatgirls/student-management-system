package org.example.repository;

import org.example.model.PartyApplicationModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyApplicationRepository extends MongoRepository<PartyApplicationModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    Optional<PartyApplicationModel> findByStudentAccountId(String studentAccountId);
    List<PartyApplicationModel> findByCurrentStage(String currentStage);
    List<PartyApplicationModel> findByAuditStatus(String auditStatus);
    List<PartyApplicationModel> findByBranch(String branch);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PartyApplicationModel> findByStudentAccountIdContaining(String studentAccountId);
    List<PartyApplicationModel> findByNameContaining(String name);
}

