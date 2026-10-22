package org.example.service;

import org.example.model.VolunteerServiceModel;
import java.util.List;

public interface VolunteerServiceService {
    // 学生操作
    VolunteerServiceModel addVolunteerService(VolunteerServiceModel volunteerService);
    VolunteerServiceModel updateVolunteerService(VolunteerServiceModel volunteerService);
    void deleteVolunteerService(String id);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。 */
    List<VolunteerServiceModel> getStudentVolunteerServices(String studentAccountId);
    VolunteerServiceModel getVolunteerServiceById(String id);

    // 管理员操作
    List<VolunteerServiceModel> getPendingVolunteerServices();
    VolunteerServiceModel auditVolunteerService(String id, String auditStatus, String auditComment, String auditorId);
    List<VolunteerServiceModel> getAllVolunteerServices();
    List<VolunteerServiceModel> searchVolunteerServices(String keyword);
    List<VolunteerServiceModel> getVolunteerServicesByType(String serviceType);
    List<VolunteerServiceModel> getVolunteerServicesByStatus(String status);
    List<VolunteerServiceModel> getVolunteerServicesByOrganization(String organization);
} 