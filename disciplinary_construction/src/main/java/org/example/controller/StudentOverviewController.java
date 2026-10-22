package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.service.StudentOverviewService;
import org.example.response.ResponseResult;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/msi/student-overview")
public class StudentOverviewController {
    private final StudentOverviewService service;
    public StudentOverviewController(StudentOverviewService service) { this.service = service; }
    @GetMapping
    public ResponseResult<Map<String, Object>> overview(
            // 学生标识统一为账号主键（见 org.example.dto.StudentRef）；学号检索走 studentIdKeyword
            @RequestParam(required = false) String studentAccountId,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String grade,
            @RequestParam(required = false) String studentIdKeyword,
            @RequestParam(required = false) String major, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, @CurrentUser Map<String, Object> user) {
        return ResponseResult.success(service.overview(studentAccountId, keyword, grade, studentIdKeyword, major, page, size, user));
    }
}
