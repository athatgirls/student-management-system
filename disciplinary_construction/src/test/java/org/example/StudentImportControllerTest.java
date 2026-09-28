package org.example;

import org.example.controller.StudentController;
import org.example.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentImportControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @Test
    void rejectsSuccessWhenNoStudentsWerePersisted() throws Exception {
        Map<String, Object> summary = summary(2, 0, 1, 1);
        when(studentService.importStudents(any(), eq("2024级"))).thenReturn(summary);

        ResponseEntity<Map<String, Object>> response = studentController.importStudents(
                new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]),
                "2024级");

        assertEquals(400, response.getBody().get("code"));
        assertEquals(summary, response.getBody().get("data"));
    }

    @Test
    void returnsSuccessWithPersistedStudentCount() throws Exception {
        Map<String, Object> summary = summary(3, 2, 0, 1);
        when(studentService.importStudents(any(), eq("2024级"))).thenReturn(summary);

        ResponseEntity<Map<String, Object>> response = studentController.importStudents(
                new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]),
                "2024级");

        assertEquals(200, response.getBody().get("code"));
        assertEquals(summary, response.getBody().get("data"));
    }

    private Map<String, Object> summary(int total, int imported, int invalid, int duplicate) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("imported", imported);
        summary.put("invalid", invalid);
        summary.put("duplicate", duplicate);
        return summary;
    }
}
