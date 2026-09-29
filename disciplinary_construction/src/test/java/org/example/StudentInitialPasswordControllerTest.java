package org.example;

import org.example.controller.StudentController;
import org.example.model.StudentModel;
import org.example.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentInitialPasswordControllerTest {
    private static final String URL = "/msi/student/change-initial-password";
    private static final String BODY = "{\"studentId\":\"test-student\",\"initialPassword\":\"OldTest123\",\"newPassword\":\"NewTest456\"}";
    @Mock private StudentService studentService;
    @Mock private org.example.service.LoginAttemptLimiter loginAttemptLimiter;
    @InjectMocks private StudentController controller;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postAndLegacyPutRejectEmptyInputWithoutDatabaseAccess() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(studentService);
    }

    @Test
    void getCannotChangePassword() throws Exception {
        RequestMapping mapping = StudentController.class.getMethod("changeInitialPassword", Map.class)
                .getAnnotation(RequestMapping.class);
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST, RequestMethod.PUT}, mapping.method());
        verifyNoInteractions(studentService);
    }

    @Test
    void missingInitialPasswordIsRejected() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":\"test-student\",\"newPassword\":\"NewTest456\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(studentService);
    }

    @Test
    void wrongInitialPasswordCannotChangePassword() throws Exception {
        when(studentService.findByStudentId("test-student")).thenReturn(new StudentModel());
        when(studentService.isDefaultPassword("test-student", "OldTest123")).thenReturn(false);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(jsonPath("$.code").value(400));
        verify(studentService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void passwordPolicyFailureIsStillReturned() throws Exception {
        allowInitialPassword();
        when(studentService.changePassword("test-student", "OldTest123", "NewTest456"))
                .thenThrow(new RuntimeException("password policy rejected"));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("password policy rejected"));
    }

    @Test
    void postPassesOriginalCredentialsToPasswordService() throws Exception {
        allowInitialPassword();
        when(studentService.changePassword("test-student", "OldTest123", "NewTest456")).thenReturn(true);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(studentService).changePassword("test-student", "OldTest123", "NewTest456");
    }

    private void allowInitialPassword() {
        when(studentService.findByStudentId("test-student")).thenReturn(new StudentModel());
        when(studentService.isDefaultPassword("test-student", "OldTest123")).thenReturn(true);
    }
}
