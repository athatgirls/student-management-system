package org.example.repository;

import org.example.model.VolunteerServiceModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerServiceRepository extends MongoRepository<VolunteerServiceModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<VolunteerServiceModel> findByStudentAccountId(String studentAccountId);
    List<VolunteerServiceModel> findByAuditStatus(String auditStatus);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<VolunteerServiceModel> findByStudentAccountIdAndAuditStatus(String studentAccountId, String auditStatus);
    List<VolunteerServiceModel> findByServiceType(String serviceType);
    List<VolunteerServiceModel> findByStatus(String status);
    List<VolunteerServiceModel> findByOrganization(String organization);
    List<VolunteerServiceModel> findByServiceNameContaining(String serviceName);
    List<VolunteerServiceModel> findByStudentNameContaining(String studentName);
} 