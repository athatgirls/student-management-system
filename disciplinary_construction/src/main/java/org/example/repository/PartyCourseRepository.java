package org.example.repository;

import org.example.model.PartyCourseModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyCourseRepository extends MongoRepository<PartyCourseModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PartyCourseModel> findByStudentAccountId(String studentAccountId);
    List<PartyCourseModel> findByAuditStatus(String auditStatus);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<PartyCourseModel> findByStudentAccountIdAndAuditStatus(String studentAccountId, String auditStatus);
    List<PartyCourseModel> findByCourseType(String courseType);
    List<PartyCourseModel> findByStatus(String status);
    List<PartyCourseModel> findByInstructor(String instructor);
    List<PartyCourseModel> findByTitleContaining(String title);
    List<PartyCourseModel> findByStudentNameContaining(String studentName);
} 