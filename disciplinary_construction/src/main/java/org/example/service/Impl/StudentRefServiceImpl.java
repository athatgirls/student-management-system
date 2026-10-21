package org.example.service.Impl;

import org.example.dto.StudentRef;
import org.example.exception.BusinessException;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.response.ResponseCode;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentRefService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 学生身份解析实现。标识约定见 {@link StudentRef}。
 */
@Service
public class StudentRefServiceImpl implements StudentRefService {

    private final StudentRepository studentRepository;
    private final CurrentUserAccessService currentUserAccessService;

    public StudentRefServiceImpl(StudentRepository studentRepository,
                                 CurrentUserAccessService currentUserAccessService) {
        this.studentRepository = studentRepository;
        this.currentUserAccessService = currentUserAccessService;
    }

    @Override
    public StudentRef current(Map<String, Object> currentUser) {
        return toRef(currentUserAccessService.requireCurrentStudent(currentUser));
    }

    @Override
    public StudentRef resolveByStudentId(String studentId) {
        StudentModel student = studentRepository.findByStudentId(studentId);
        if (student == null) {
            throw new BusinessException(ResponseCode.STUDENT_NOT_FOUND);
        }
        return toRef(student);
    }

    @Override
    public StudentRef resolveByAccountId(String accountId) {
        StudentModel student = studentRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ResponseCode.STUDENT_NOT_FOUND));
        return toRef(student);
    }

    private StudentRef toRef(StudentModel student) {
        return new StudentRef(student.getId(), student.getStudentId(), student.getName());
    }
}
