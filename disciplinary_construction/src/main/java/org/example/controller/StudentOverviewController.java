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
    public ResponseResult<Map<String, Object>> overview(@RequestParam(required = false) String studentId,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String grade,
            @RequestParam(required = false) String major, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, @CurrentUser Map<String, Object> user) {
        return ResponseResult.success(service.overview(studentId, keyword, grade, major, page, size, user));
    }
}
