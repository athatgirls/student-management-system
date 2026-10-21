package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.GradeController;
import org.example.model.GradeModel;
import org.example.model.StudyRecordModel;
import org.example.model.StudentModel;
import org.example.repository.StudyRecordRepository;
import org.example.repository.StudentRepository;
import org.example.service.GradeService;
import org.example.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GradeControllerTest {

    @Mock
    private GradeService gradeService;

    @Mock
    private StudentService studentService;

    @Mock
    private StudyRecordRepository studyRecordRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private org.example.service.StudentRefService studentRefService;

    @InjectMocks
    private GradeController gradeController;

    @Test
    void createRejectsGradeAlreadyShownInList() throws Exception {
        when(studentService.getAllGrades()).thenReturn(Arrays.asList("2024级", "2027级"));

        ResponseEntity<Map<String, Object>> response = gradeController.createGrade(body("2027"));

        assertEquals(400, response.getBody().get("code"));
        verify(gradeService, never()).createGrade(any(GradeModel.class));
    }

    @Test
    void createPersistsNormalizedGradeName() throws Exception {
        when(studentService.getAllGrades()).thenReturn(Collections.emptyList());
        when(gradeService.createGrade(any(GradeModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = gradeController.createGrade(body("2027"));

        assertEquals(200, response.getBody().get("code"));
        assertEquals("2027级", ((GradeModel) response.getBody().get("data")).getGradeName());
    }

    @Test
    void deleteByNameRejectsDefaultGrade() throws Exception {
        ResponseEntity<Map<String, Object>> response = gradeController.deleteGradeByName("2024级");

        assertEquals(400, response.getBody().get("code"));
        verify(gradeService, never()).deleteGrade(any(String.class));
    }

    @Test
    void deleteByNameRemovesManagedGrade() throws Exception {
        GradeModel managed = new GradeModel();
        managed.setId("g1");
        managed.setGradeName("2027级");
        when(gradeService.findByGradeName("2027级")).thenReturn(managed);
        when(gradeService.deleteGrade("g1")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = gradeController.deleteGradeByName("2027");

        assertEquals(200, response.getBody().get("code"));
        verify(gradeService).deleteGrade("g1");
    }

    @Test
    void onlyAdminCanClassifyExistingRecordWithoutChangingTheGrade() {
        StudyRecordModel record = new StudyRecordModel();
        record.setId("record-1"); record.setScore(85); record.setCredit(2.0);
        when(studyRecordRepository.findById("record-1")).thenReturn(Optional.of(record));

        assertThrows(ResponseStatusException.class, () -> gradeController.setCourseType("record-1",
                Map.of("courseType", "degree"), Map.of("userType", "student")));
        assertThrows(ResponseStatusException.class, () -> gradeController.setCourseType("record-1",
                Map.of("courseType", "unknown"), Map.of("userType", "admin")));
        assertEquals(200, gradeController.setCourseType("record-1", Map.of("courseType", "degree"),
                Map.of("userType", "admin")).getBody().get("code"));
        assertEquals("degree", record.getCourseType());
        assertEquals(85, record.getScore());
        verify(studyRecordRepository).save(record);
    }

    @Test
    void reimportWithoutCourseTypeKeepsExistingClassification() {
        StudyRecordModel old = new StudyRecordModel(); old.setCourseType("degree");
        // 导入行学号仅作匹配，经 resolveByStudentId 解析出关联键后查重（见 org.example.dto.StudentRef）
        when(studentRefService.resolveByStudentId("2026001"))
                .thenReturn(new org.example.dto.StudentRef("student-db-id", "2026001", "学生甲"));
        when(studyRecordRepository.findByStudentAccountIdAndSemesterAndCourse("student-db-id", "2026秋", "数学"))
                .thenReturn(Optional.of(old));
        ReflectionTestUtils.setField(gradeController, "objectMapper", new ObjectMapper());

        String json = "[{\"studentId\":\"2026001\",\"semester\":\"2026秋\",\"courseName\":\"数学\",\"score\":90,\"credit\":2}]";
        ResponseEntity<Map<String, Object>> result = gradeController.importGrades(json, null, Map.of("userType", "admin"));
        assertEquals(200, result.getBody().get("code"));
        assertEquals("degree", old.getCourseType());
        verify(studyRecordRepository).save(old);
    }

    private Map<String, Object> body(String gradeName) {
        Map<String, Object> body = new HashMap<>();
        body.put("gradeName", gradeName);
        return body;
    }
}
