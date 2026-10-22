package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.StudyRecordModel;
import org.example.repository.StudyRecordRepository;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentRefService;
import org.example.util.SubmissionValidation;
import org.example.util.StudyCourseType;
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
    private final StudentRefService studentRefService;
    public StudyRecordController(StudyRecordRepository repository, CurrentUserAccessService access,
                                 StudentRefService studentRefService) {
        this.repository = repository; this.access = access; this.studentRefService = studentRefService;
    }
    @PostMapping
    public StudyRecordModel save(@RequestBody StudyRecordModel input, @CurrentUser Map<String, Object> user) {
        StudentRef ref = studentRefService.current(user);
        if (input.getId() != null) {
            StudyRecordModel old = repository.findById(input.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            access.requireStudentRecord(user, old.getStudentAccountId());
            if (!"student".equals(old.getSource()))
                throw new AccessDeniedException("只能编辑本人填写的学习记录，教务成绩请联系管理员更正");
        }
        try {
            SubmissionValidation.required(input.getCourse(), "课程名称");
            SubmissionValidation.required(input.getSemester(), "学期");
            SubmissionValidation.attachments(input.getAttachments());
            input.setCourseType(StudyCourseType.requireValid(input.getCourseType()));
            if (input.getScore() == null || input.getScore() < 0 || input.getScore() > 100
                    || input.getCredit() == null || !Double.isFinite(input.getCredit()) || input.getCredit() <= 0)
                throw new IllegalArgumentException("成绩应在 0 至 100 之间，学分必须大于 0");
        } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        input.setStudentAccountId(ref.getAccountId()); input.setStudentId(ref.getStudentId());
        input.setSource("student"); input.setStatus("学生自填");
        return repository.save(input);
    }
}
