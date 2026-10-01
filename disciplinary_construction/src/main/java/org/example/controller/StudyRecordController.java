package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.StudyRecordModel;
import org.example.model.StudentModel;
import org.example.repository.StudyRecordRepository;
import org.example.service.CurrentUserAccessService;
import org.example.util.SubmissionValidation;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/msi/study-records")
public class StudyRecordController {
    private final StudyRecordRepository repository;
    private final CurrentUserAccessService access;
    public StudyRecordController(StudyRecordRepository repository, CurrentUserAccessService access) {
        this.repository = repository; this.access = access;
    }
    @PostMapping
    public StudyRecordModel save(@RequestBody StudyRecordModel input, @CurrentUser Map<String, Object> user) {
        StudentModel student = access.requireCurrentStudent(user);
        if (input.getId() != null) {
            StudyRecordModel old = repository.findById(input.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!student.getId().equals(old.getStudentDbId()) || !"student".equals(old.getSource()))
                throw new AccessDeniedException("只能编辑本人填写的学习记录，教务成绩请联系管理员更正");
        }
        try {
            SubmissionValidation.required(input.getCourse(), "课程名称");
            SubmissionValidation.required(input.getSemester(), "学期");
            SubmissionValidation.attachments(input.getAttachments());
            if (input.getScore() == null || input.getScore() < 0 || input.getScore() > 100
                    || input.getCredit() == null || !Double.isFinite(input.getCredit()) || input.getCredit() < 0)
                throw new IllegalArgumentException("成绩应在 0 至 100 之间，学分不能为负数");
        } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        input.setStudentDbId(student.getId()); input.setStudentId(student.getStudentId());
        input.setSource("student"); input.setStatus("学生自填");
        return repository.save(input);
    }
}
