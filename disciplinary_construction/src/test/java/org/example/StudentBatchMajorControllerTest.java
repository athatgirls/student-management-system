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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentBatchMajorControllerTest {

    @Mock
    private StudentService studentService;

    @Mock
    private org.example.service.CurrentUserAccessService currentUserAccessService;

    @InjectMocks
    private StudentController studentController;

    @Test
    void postUpdatesSelectedStudentsAndLegacyPutIsNotAllowed() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(studentController).build();
        when(studentService.batchUpdateMajorByIds(Arrays.asList("student-1", "student-2"), "人工智能")).thenReturn(2);
        String body = "{\"ids\":[\"student-1\",\"student-2\"],\"major\":\" 人工智能 \"}";
        mvc.perform(post("/msi/student/batch-update-major").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.updatedCount").value(2));
        mvc.perform(put("/msi/student/batch-update-major").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isMethodNotAllowed());
        verify(studentService).batchUpdateMajorByIds(Arrays.asList("student-1", "student-2"), "人工智能");
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
        org.example.aspect.CurrentUserAspect identity = mock(org.example.aspect.CurrentUserAspect.class);
        when(identity.supportsParameter(any())).thenCallRealMethod();
        when(identity.resolveArgument(any(), any(), any(), any()))
                .thenReturn(Map.of("userId", "admin", "userType", "admin"));
        MockMvcBuilders.standaloneSetup(studentController).setCustomArgumentResolvers(identity).build()
                .perform(get("/msi/student/batch-update-major").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"student-1\"],\"major\":\"人工智能\"}"));
        verify(studentService, never()).batchUpdateMajorByIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void batchUpdateMajorRejectsEmptyIds() {
        ResponseEntity<Map<String, Object>> response = studentController.batchUpdateMajor(request(Collections.emptyList(), "人工智能"));

        assertEquals(400, response.getBody().get("code"));
        verify(studentService, never()).batchUpdateMajorByIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void batchUpdateMajorRejectsBlankMajor() {
        ResponseEntity<Map<String, Object>> response = studentController.batchUpdateMajor(request(Arrays.asList("student-1"), "  "));

        assertEquals(400, response.getBody().get("code"));
        verify(studentService, never()).batchUpdateMajorByIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    private Map<String, Object> request(java.util.List<String> ids, String major) {
        Map<String, Object> request = new HashMap<>();
        request.put("ids", ids);
        request.put("major", major);
        return request;
    }
}
