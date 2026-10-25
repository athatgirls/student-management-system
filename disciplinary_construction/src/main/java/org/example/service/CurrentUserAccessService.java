package org.example.service;

import org.example.dto.StudentRef;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 学生数据归属校验入口。标识约定见 {@link StudentRef}。
 *
 * <p>校验器与场景的对应关系，勿混用：
 * <ul>
 *   <li>{@link #requireSelf}：学生账号资源（如 GET /student/{id}），参数是账号主键。</li>
 *   <li>{@link #requireStudentRecord}：业务记录归属校验，参数是记录的 studentAccountId。</li>
 *   <li>管理端按学号指定目标学生用 {@link #requireAdminTarget}（或
 *       {@link StudentRefService#resolveByStudentId}），禁止调用方自行把学号当关联键。</li>
 * </ul>
 */
@Service
public class CurrentUserAccessService {

    private final StudentRepository studentRepository;

    public CurrentUserAccessService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public boolean isAdmin(Map<String, Object> currentUser) {
        return currentUser != null && "admin".equals(currentUser.get("userType"));
    }

    /**
     * JWT userId 即账号 Mongo 主键（对学生即 studentAccountId），学生/管理员通用。
     * 仅用于识别当前登录账号，禁止当作学号使用。
     */
    public String requireUserId(Map<String, Object> currentUser) {
        Object userId = currentUser != null ? currentUser.get("userId") : null;
        if (!(userId instanceof String) || ((String) userId).trim().isEmpty()) {
            throw new AccessDeniedException("无法识别当前用户");
        }
        return (String) userId;
    }

    public StudentModel requireCurrentStudent(Map<String, Object> currentUser) {
        if (currentUser == null || !"student".equals(currentUser.get("userType"))) {
            throw new AccessDeniedException("当前操作仅限学生账号");
        }
        return studentRepository.findById(requireUserId(currentUser))
                .orElseThrow(() -> new AccessDeniedException("当前学生账号不存在"));
    }

    /** 当前学生的学号（studentId）。仅用于展示/导入匹配，禁止用于关联查询。 */
    public String requireCurrentStudentId(Map<String, Object> currentUser) {
        StudentModel student = requireCurrentStudent(currentUser);
        if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
            throw new AccessDeniedException("当前学生账号缺少学号");
        }
        return student.getStudentId();
    }

    /** 账号资源本人校验（如 GET /student/{id}），参数是账号主键。业务记录归属请用 {@link #requireStudentRecord}。 */
    public void requireSelf(Map<String, Object> currentUser, String accountId) {
        requireAccountMatch(currentUser, accountId);
    }

    /** 业务记录归属校验，参数是记录的 studentAccountId（账号主键）。不要传学号。 */
    public void requireStudentRecord(Map<String, Object> currentUser, String studentAccountId) {
        requireAccountMatch(currentUser, studentAccountId);
    }

    /**
     * 管理端指定目标学生：要求管理员登录，按学号解析身份（禁止调用方自行转换）。
     */
    public StudentRef requireAdminTarget(Map<String, Object> currentUser, String studentId) {
        if (!isAdmin(currentUser)) {
            throw new AccessDeniedException("当前操作仅限管理员账号");
        }
        StudentModel student = studentRepository.findByStudentId(studentId);
        if (student == null) {
            throw new AccessDeniedException("目标学生不存在");
        }
        return new StudentRef(student.getId(), student.getStudentId(), student.getName());
    }

    private void requireAccountMatch(Map<String, Object> currentUser, String studentAccountId) {
        if (isAdmin(currentUser)) {
            return;
        }
        StudentModel student = requireCurrentStudent(currentUser);
        if (!equalsNonBlank(student.getId(), studentAccountId)) {
            throw new AccessDeniedException("无权访问其他学生的数据");
        }
    }

    private boolean equalsNonBlank(String expected, String actual) {
        return expected != null && !expected.trim().isEmpty() && expected.equals(actual);
    }
}
