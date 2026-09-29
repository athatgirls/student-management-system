package org.example;

import org.example.controller.StudentController;
import org.example.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentBatchMajorControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @Test
    void postAndLegacyPutBothUpdateTheSelectedStudents() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(studentController).build();
        when(studentService.batchUpdateMajorByIds(Arrays.asList("student-1"), "人工智能")).thenReturn(1);
        String body = "{\"ids\":[\"student-1\"],\"major\":\" 人工智能 \"}";
        mvc.perform(post("/msi/student/batch-update-major").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.updatedCount").value(1));
        mvc.perform(put("/msi/student/batch-update-major").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.updatedCount").value(1));
        verify(studentService, times(2)).batchUpdateMajorByIds(Arrays.asList("student-1"), "人工智能");
    }

    @Test
    void postRejectsEmptyInputWithoutWritingStudents() throws Exception {
        MockMvcBuilders.standaloneSetup(studentController).build()
                .perform(post("/msi/student/batch-update-major").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(studentService);
    }

    @Test
    void getCannotPerformBatchUpdate() throws Exception {
        // GET may match the separate /{id} reader, but must never map to this writer.
        RequestMapping mapping = StudentController.class.getMethod("batchUpdateMajor", Map.class)
                .getAnnotation(RequestMapping.class);
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST, RequestMethod.PUT}, mapping.method());
        verifyNoInteractions(studentService);
    }

    @Test
    void batchUpdateMajorRejectsEmptyIds() {
        ResponseEntity<Map<String, Object>> response = studentController.batchUpdateMajor(request(Collections.emptyList(), "人工智能"));

        assertEquals(400, response.getBody().get("code"));
        assertEquals("请先选择学生", response.getBody().get("msg"));
        verify(studentService, never()).batchUpdateMajorByIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void batchUpdateMajorRejectsBlankMajor() {
        ResponseEntity<Map<String, Object>> response = studentController.batchUpdateMajor(request(Arrays.asList("student-1"), "  "));

        assertEquals(400, response.getBody().get("code"));
        assertEquals("请选择专业", response.getBody().get("msg"));
        verify(studentService, never()).batchUpdateMajorByIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void batchUpdateMajorReturnsUpdatedCount() {
        when(studentService.batchUpdateMajorByIds(Arrays.asList("student-1", "student-2"), "人工智能")).thenReturn(2);

        ResponseEntity<Map<String, Object>> response = studentController.batchUpdateMajor(request(Arrays.asList("student-1", "student-2"), " 人工智能 "));

        assertEquals(200, response.getBody().get("code"));
        assertEquals(2, ((Map<?, ?>) response.getBody().get("data")).get("updatedCount"));
        assertEquals("批量修改专业成功", response.getBody().get("msg"));
        verify(studentService).batchUpdateMajorByIds(Arrays.asList("student-1", "student-2"), "人工智能");
    }

    private Map<String, Object> request(java.util.List<String> ids, String major) {
        Map<String, Object> request = new HashMap<>();
        request.put("ids", ids);
        request.put("major", major);
        return request;
    }
}
