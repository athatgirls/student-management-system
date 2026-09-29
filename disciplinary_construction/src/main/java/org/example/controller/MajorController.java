package org.example.controller;

import org.example.model.MajorModel;
import org.example.repository.StudentRepository;
import org.example.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/msi/major")
public class MajorController {

    @Autowired
    private MajorService majorService;

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createMajor(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String majorName = normalize((String) requestBody.get("majorName"));
            if (majorName.isEmpty()) {
                return failure(result, "请选择专业");
            }
            if (majorService.getAllMajorNames().contains(majorName)) {
                return failure(result, "该专业已存在");
            }

            MajorModel major = new MajorModel();
            major.setMajorName(majorName);
            result.put("code", 200);
            result.put("data", majorService.createMajor(major));
            result.put("msg", "专业添加成功");
        } catch (RuntimeException e) {
            return failure(result, e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Create major failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllMajors() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("data", majorService.getAllMajorNames());
            result.put("msg", "专业列表获取成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Load major list failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete-by-name")
    public ResponseEntity<Map<String, Object>> deleteMajorByName(@RequestParam("majorName") String majorName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String normalized = normalize(majorName);
            if (normalized.isEmpty()) {
                return failure(result, "请选择专业");
            }
            long studentCount = studentRepository.countByMajor(normalized);
            if (studentCount > 0) {
                return failure(result, "该专业下仍有 " + studentCount + " 名学生，无法删除");
            }
            MajorModel existing = majorService.findByMajorName(normalized);
            if (existing == null) {
                return failure(result, "该专业不存在或非手动添加，无法删除");
            }
            if (!majorService.deleteMajor(existing.getId())) {
                return failure(result, "该专业不存在或非手动添加，无法删除");
            }
            result.put("code", 200);
            result.put("data", null);
            result.put("msg", "专业已删除");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Delete major failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> failure(Map<String, Object> result, String message) {
        result.put("code", 400);
        result.put("data", null);
        result.put("msg", message);
        return ResponseEntity.ok(result);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
