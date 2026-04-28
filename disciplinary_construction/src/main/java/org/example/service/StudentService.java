package org.example.service;

import org.example.model.StudentModel;
import java.util.List;

public interface StudentService {
    // 登录
    StudentModel login(String account, String password);
    
    // 检查密码是否为初始密码
    boolean isDefaultPassword(String studentId, String password);
    
    // 获取所有学生
    List<StudentModel> findAllStudents();
    
    // 根据ID查找学生
    StudentModel findById(String id);
    
    // 创建学生
    StudentModel createStudent(StudentModel student);
    
    // 更新学生信息
    StudentModel updateStudent(StudentModel student);
    
    // 删除学生
    boolean deleteStudent(String id);
    
    // 根据学号查找学生
    StudentModel findByStudentId(String studentId);
    
    // 根据姓名查找学生
    List<StudentModel> findByName(String name);
    
    // 根据专业查找学生
    List<StudentModel> findByMajor(String major);
    
    // 根据年级查找学生
    List<StudentModel> findByGrade(String grade);

    // 批量导入学生
    void importStudents(java.io.InputStream inputStream, String grade) throws Exception;

    // 分页搜索学生
    java.util.Map<String, Object> findStudentsPage(int page, int size, String name, String studentId, String major, String grade);
    
    // 获取所有年级列表
    List<String> getAllGrades();
    
    // 批量修改学生状态（按年级）
    int batchUpdateStatusByGrade(String grade, String status, String statusRemark);
    
    // 管理员重置学生密码
    boolean resetStudentPassword(String studentId, String newPassword);
    
    // 学生修改自己的密码
    boolean changePassword(String studentId, String oldPassword, String newPassword);
}
