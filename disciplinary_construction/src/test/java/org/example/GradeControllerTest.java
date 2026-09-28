package org.example;

import org.example.controller.GradeController;
import org.example.model.GradeModel;
import org.example.service.GradeService;
import org.example.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeControllerTest {

    @Mock
    private GradeService gradeService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private GradeController gradeController;

    @Test
    void createRejectsGradeAlreadyShownInList() throws Exception {
        when(studentService.getAllGrades()).thenReturn(Arrays.asList("2024级", "2027级"));

        ResponseEntity<Map<String, Object>> response = gradeController.createGrade(body("2027"));

        assertEquals(400, response.getBody().get("code"));
        assertEquals("该年级已存在", response.getBody().get("msg"));
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
        assertEquals("默认年级不可删除", response.getBody().get("msg"));
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

    private Map<String, Object> body(String gradeName) {
        Map<String, Object> body = new HashMap<>();
        body.put("gradeName", gradeName);
        return body;
    }
}
