package org.example.service;

import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Centralizes ownership checks for endpoints that expose student-scoped data.
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

    public String requireStudentNumber(Map<String, Object> currentUser) {
        StudentModel student = requireCurrentStudent(currentUser);
        if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
            throw new AccessDeniedException("当前学生账号缺少学号");
        }
        return student.getStudentId();
    }

    public void requireStudentAccess(Map<String, Object> currentUser, String studentIdentifier) {
        if (isAdmin(currentUser)) {
            return;
        }
        StudentModel student = requireCurrentStudent(currentUser);
        if (!equalsNonBlank(student.getId(), studentIdentifier)
                && !equalsNonBlank(student.getStudentId(), studentIdentifier)) {
            throw new AccessDeniedException("无权访问其他学生的数据");
        }
    }

    private boolean equalsNonBlank(String expected, String actual) {
        return expected != null && !expected.trim().isEmpty() && expected.equals(actual);
    }
}
