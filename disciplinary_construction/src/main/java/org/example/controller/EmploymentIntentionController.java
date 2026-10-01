package org.example.controller;
import org.example.annotation.CurrentUser;
import org.example.model.*;
import org.example.repository.EmploymentIntentionRepository;
import org.example.service.CurrentUserAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
@RestController
@RequestMapping("/msi/employment-intentions")
public class EmploymentIntentionController {
    private final EmploymentIntentionRepository repository;
    private final CurrentUserAccessService access;
    public EmploymentIntentionController(EmploymentIntentionRepository repository, CurrentUserAccessService access) {
        this.repository = repository; this.access = access;
    }
    @GetMapping("/me")
    public EmploymentIntentionModel me(@CurrentUser Map<String,Object> user) {
        return repository.findById(access.requireCurrentStudent(user).getId()).orElseGet(EmploymentIntentionModel::new);
    }
    @PostMapping("/me")
    public EmploymentIntentionModel save(@RequestBody EmploymentIntentionModel input, @CurrentUser Map<String,Object> user) {
        StudentModel student = access.requireCurrentStudent(user);
        if (input.getIntentionType() == null || input.getIntentionType().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择意向方向");
        input.setId(student.getId()); input.setStudentId(student.getStudentId()); input.setStudentName(student.getName());
        input.setUpdateTime(LocalDateTime.now()); return repository.save(input);
    }
    @GetMapping("/admin/list")
    public List<EmploymentIntentionModel> list() { return repository.findAll(); }
}
