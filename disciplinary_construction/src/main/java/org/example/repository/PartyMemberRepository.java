package org.example.repository;

import org.example.model.PartyMemberModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyMemberRepository extends MongoRepository<PartyMemberModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PartyMemberModel> findByStudentAccountId(String studentAccountId);
    List<PartyMemberModel> findByAuditStatus(String auditStatus);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PartyMemberModel> findByStudentAccountIdAndAuditStatus(String studentAccountId, String auditStatus);
    List<PartyMemberModel> findByBranch(String branch);
    List<PartyMemberModel> findByIsRegular(String isRegular);
    List<PartyMemberModel> findByNameContaining(String name);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PartyMemberModel> findByStudentAccountIdContaining(String studentAccountId);
} 