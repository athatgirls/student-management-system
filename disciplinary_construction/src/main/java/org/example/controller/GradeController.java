package org.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.annotation.CurrentUser;
import org.example.dto.StudyRecordImportItem;
import org.example.model.GradeModel;
import org.example.model.StudentModel;
import org.example.model.StudyRecordModel;
import org.example.repository.StudentRepository;
import org.example.repository.StudyRecordRepository;
import org.example.service.GradeService;
import org.example.service.StudentService;
import org.example.util.StudentGradePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msi/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createGrade(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String gradeName = StudentGradePolicy.requireValid((String) requestBody.get("gradeName"));
            if (studentService.getAllGrades().contains(gradeName)) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "该年级已存在");
                return ResponseEntity.ok(result);
            }

            GradeModel grade = new GradeModel();
            grade.setGradeName(gradeName);
            GradeModel createdGrade = gradeService.createGrade(grade);
            result.put("code", 200);
            result.put("data", createdGrade);
            result.put("msg", "年级添加成功");
        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("data", null);
            result.put("msg", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Create grade failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllGrades() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> gradeNames = gradeService.getAllGrades().stream()
                    .map(GradeModel::getGradeName)
                    .map(StudentGradePolicy::normalize)
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
            StudentGradePolicy.DEFAULT_GRADES.forEach(grade -> {
                if (!gradeNames.contains(grade)) {
                    gradeNames.add(grade);
                }
            });
            gradeNames.sort(String::compareTo);
            result.put("code", 200);
            result.put("data", gradeNames);
            result.put("msg", "Grade list loaded");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Load grade list failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteGrade(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = gradeService.deleteGrade(id);
            result.put("code", success ? 200 : 404);
            result.put("data", null);
            result.put("msg", success ? "Grade deleted" : "Grade not found");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Delete grade failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete-by-name")
    public ResponseEntity<Map<String, Object>> deleteGradeByName(@RequestParam("gradeName") String gradeName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String normalized = StudentGradePolicy.requireValid(gradeName);
            if (StudentGradePolicy.DEFAULT_GRADES.contains(normalized)) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "默认年级不可删除");
                return ResponseEntity.ok(result);
            }
            GradeModel existing = gradeService.findByGradeName(normalized);
            if (existing == null) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "该年级不是手动添加的，无法删除");
                return ResponseEntity.ok(result);
            }
            boolean success = gradeService.deleteGrade(existing.getId());
            result.put("code", success ? 200 : 404);
            result.put("data", null);
            result.put("msg", success ? "年级已删除；若仍有学生属于该年级，列表中会继续显示" : "Grade not found");
        } catch (RuntimeException e) {
            result.put("code", 400);
            result.put("data", null);
            result.put("msg", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Delete grade failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importGrades(
            @RequestParam(value = "records") String recordsJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String userType = (String) currentUser.get("userType");
            if (!"admin".equals(userType)) {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "Only admins can import grades");
                return ResponseEntity.ok(result);
            }

            List<StudyRecordImportItem> records = objectMapper.readValue(
                    recordsJson, new TypeReference<List<StudyRecordImportItem>>() {}
            );

            if (records == null || records.isEmpty()) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "Import data is empty");
                return ResponseEntity.ok(result);
            }

            int createdCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;
            List<String> missingStudents = new ArrayList<>();
            List<String> invalidRows = new ArrayList<>();

            for (StudyRecordImportItem item : records) {
                String studentId = trim(item.getStudentId());
                String semester = trim(item.getSemester());
                String courseName = trim(item.getCourseName());

                if (studentId.isEmpty() || semester.isEmpty() || courseName.isEmpty() || item.getScore() == null) {
                    skippedCount++;
                    invalidRows.add(String.format("studentId[%s] course[%s] has incomplete data", studentId, courseName));
                    continue;
                }

                Integer score = parseInteger(item.getScore());
                if (score == null) {
                    skippedCount++;
                    invalidRows.add(String.format("studentId[%s] course[%s] has invalid score", studentId, courseName));
                    continue;
                }

                StudentModel student = studentRepository.findByStudentId(studentId);
                if (student == null) {
                    skippedCount++;
                    missingStudents.add(studentId);
                    continue;
                }

                Optional<StudyRecordModel> existing = studyRecordRepository
                        .findByStudentIdAndSemesterAndCourse(studentId, semester, courseName);

                StudyRecordModel studyRecord = existing.orElseGet(StudyRecordModel::new);
                studyRecord.setStudentId(studentId);
                studyRecord.setStudentDbId(student.getId());
                studyRecord.setSemester(semester);
                studyRecord.setCourse(courseName);
                studyRecord.setScore(score);
                studyRecord.setSource("official");
                studyRecord.setCredit(parseDouble(item.getCredit()));
                studyRecord.setStatus(resolveStatus(score, trim(item.getStatus())));

                studyRecordRepository.save(studyRecord);
                if (existing.isPresent()) {
                    updatedCount++;
                } else {
                    createdCount++;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("filename", file != null ? file.getOriginalFilename() : null);
            data.put("total", records.size());
            data.put("createdCount", createdCount);
            data.put("updatedCount", updatedCount);
            data.put("skippedCount", skippedCount);
            data.put("missingStudents", missingStudents.stream().distinct().collect(Collectors.toList()));
            data.put("invalidRows", invalidRows);

            result.put("code", 200);
            result.put("data", data);
            result.put("msg", "Grades imported");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Import grades failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getGradeRecords(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String semester,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String userType = (String) currentUser.get("userType");
            String effectiveStudentId = trim(studentId);

            if ("student".equals(userType)) {
                String studentDbId = (String) currentUser.get("userId");
                StudentModel currentStudent = studentRepository.findById(studentDbId).orElse(null);
                if (currentStudent == null) {
                    result.put("code", 404);
                    result.put("data", null);
                    result.put("msg", "Current student not found");
                    return ResponseEntity.ok(result);
                }
                effectiveStudentId = currentStudent.getStudentId();
            }

            List<StudyRecordModel> records;
            String cleanSemester = trim(semester);
            if (!effectiveStudentId.isEmpty() && !cleanSemester.isEmpty()) {
                records = studyRecordRepository.findByStudentIdAndSemester(effectiveStudentId, cleanSemester);
            } else if (!effectiveStudentId.isEmpty()) {
                records = studyRecordRepository.findByStudentId(effectiveStudentId);
            } else if (!cleanSemester.isEmpty()) {
                records = studyRecordRepository.findBySemester(cleanSemester);
            } else {
                records = studyRecordRepository.findAll();
            }

            result.put("code", 200);
            result.put("data", records);
            result.put("msg", "Grade records loaded");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Load grade records failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer parseInteger(Object value) {
        try {
            if (value == null) {
                return null;
            }
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String text = value.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            return (int) Math.round(Double.parseDouble(text));
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(Object value) {
        try {
            if (value == null) {
                return null;
            }
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            String text = value.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            return Double.parseDouble(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveStatus(Integer score, String status) {
        if (!status.isEmpty()) {
            return status;
        }
        if (score == null) {
            return "not_entered";
        }
        return score >= 60 ? "passed" : "failed";
    }
}
