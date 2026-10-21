package org.example.service;

import org.example.model.PartyCourseModel;
import java.util.List;

public interface PartyCourseService {
    // 学生操作
    PartyCourseModel addPartyCourse(PartyCourseModel partyCourse);
    PartyCourseModel updatePartyCourse(PartyCourseModel partyCourse);
    void deletePartyCourse(String id);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。 */
    List<PartyCourseModel> getStudentPartyCourses(String studentAccountId);
    PartyCourseModel getPartyCourseById(String id);

    // 管理员操作
    List<PartyCourseModel> getPendingPartyCourses();
    PartyCourseModel auditPartyCourse(String id, String auditStatus, String auditComment, String auditorId);
    List<PartyCourseModel> getAllPartyCourses();
    List<PartyCourseModel> searchPartyCourses(String keyword);
    List<PartyCourseModel> getPartyCoursesByType(String courseType);
    List<PartyCourseModel> getPartyCoursesByStatus(String status);
    List<PartyCourseModel> getPartyCoursesByInstructor(String instructor);
} 