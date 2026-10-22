package org.example.service;

import org.example.model.StudentModel;
import java.util.List;

/**
 * 学生数据管理：账号 CRUD、名单导入、学号/姓名/专业/年级检索等。
 *
 * <p>职责边界（勿混淆）：本服务只管「学生数据怎么存取」。需要学生身份
 * （当前登录者是谁、业务记录挂到谁名下）时一律走 {@link StudentRefService}，
 * 禁止把本服务查出的 {@link StudentModel#getStudentId()}（学号）当业务关联键使用。
 * 标识约定见 {@link org.example.dto.StudentRef}。
 */
public interface StudentService {
    // 登录
    StudentModel login(String account, String password);
    
    // 检查密码是否为初始密码
    boolean isDefaultPassword(String studentId, String password);
    
    // 获取所有学生
    List<StudentModel> findAllStudents();
    
    // 根据ID（账号主键）查找学生；账号主键即业务关联键 studentAccountId
    StudentModel findById(String id);
    
    // 创建学生
    StudentModel createStudent(StudentModel student);
    
    // 更新学生信息
    StudentModel updateStudent(StudentModel student);
    
    // 删除学生
    boolean deleteStudent(String id);
    
    // 根据学号查找学生（学号仅用于展示/导入匹配/管理端指定目标，不得用于业务关联查询）
    StudentModel findByStudentId(String studentId);
    
    // 根据姓名查找学生
    List<StudentModel> findByName(String name);
    
    // 根据专业查找学生
    List<StudentModel> findByMajor(String major);
    
    // 根据年级查找学生
    List<StudentModel> findByGrade(String grade);

    // 批量导入学生
    java.util.Map<String, Object> importStudents(java.io.InputStream inputStream, String grade) throws Exception;

    // 分页搜索学生
    java.util.Map<String, Object> findStudentsPage(int page, int size, String name, String studentId, String major, String grade);
    
    // 获取所有年级列表
    List<String> getAllGrades();
    
    // 批量修改学生状态（按年级）
    int batchUpdateStatusByGrade(String grade, String status, String statusRemark);

    int batchUpdateMajorByIds(List<String> ids, String major);
    
    // 管理员重置学生密码
    boolean resetStudentPassword(String studentId, String newPassword);
    
    // 学生修改自己的密码
    boolean changePassword(String studentId, String oldPassword, String newPassword);
}
