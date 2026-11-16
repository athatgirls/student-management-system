package org.example;

import io.jsonwebtoken.Claims;
import org.example.controller.StudentController;
import org.example.model.StudentModel;
import org.example.service.StudentService;
import org.example.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Date;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentInitialPasswordControllerTest {
    private static final String URL = "/msi/student/change-initial-password";
    private static final String BODY = "{\"studentId\":\"test-student\",\"initialPassword\":\"OldTest123\",\"newPassword\":\"NewTest456\"}";
    @Mock private StudentService studentService;
    @Mock private JwtUtil jwtUtil;
    @Mock private org.example.service.CurrentUserAccessService currentUserAccessService;
    @Mock private org.example.service.LoginAttemptLimiter loginAttemptLimiter;
    @InjectMocks private StudentController controller;
    private MockMvc mvc;
    private final StudentModel student = new StudentModel();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postRejectsEmptyInputAndLegacyPutIsNotAllowed() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(studentService);
    }

    @Test
    void getCannotChangePassword() throws Exception {
        org.example.aspect.CurrentUserAspect identity = mock(org.example.aspect.CurrentUserAspect.class);
        when(identity.supportsParameter(any())).thenCallRealMethod();
        when(identity.resolveArgument(any(), any(), any(), any()))
                .thenReturn(java.util.Map.of("userId", "admin", "userType", "admin"));
        MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(identity).build()
                .perform(get(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        verify(studentService).findById("change-initial-password");
        verify(studentService, never()).findByStudentId(anyString());
        verify(studentService, never()).changePassword(anyString(), anyString(), anyString());
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
        when(studentService.findByStudentId("test-student")).thenReturn(student);
        when(studentService.isDefaultPassword(student, "OldTest123")).thenReturn(false);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(jsonPath("$.code").value(400));
        verify(studentService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void passwordPolicyFailureIsStillReturned() throws Exception {
        allowInitialPassword();
        when(studentService.replaceInitialPassword(student, "NewTest456", null))
                .thenThrow(new IllegalArgumentException("password policy rejected"));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("password policy rejected"));
    }

    @Test
    void legacyPostStillAcceptsOriginalCredentials() throws Exception {
        allowInitialPassword();
        when(studentService.replaceInitialPassword(student, "NewTest456", null)).thenReturn(true);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(studentService).replaceInitialPassword(student, "NewTest456", null);
    }

    private void allowInitialPassword() {
        when(studentService.findByStudentId("test-student")).thenReturn(student);
        when(studentService.isDefaultPassword(student, "OldTest123")).thenReturn(true);
    }

    @Test
    void grantIsBoundToTheAuthenticatedAccountAndCannotBeReplayed() throws Exception {
        Claims grant = mock(Claims.class);
        Date expiry = new Date(System.currentTimeMillis() + 60_000);
        when(grant.getSubject()).thenReturn("account-id");
        when(grant.getExpiration()).thenReturn(expiry);
        student.setPassword("original-hash");
        when(jwtUtil.readInitialPasswordToken("grant")).thenReturn(grant);
        when(studentService.findById("account-id")).thenReturn(student);
        when(jwtUtil.matchesInitialPasswordVersion(grant, "original-hash")).thenReturn(true);
        when(studentService.replaceInitialPassword(student, "NewTest456", expiry)).thenAnswer(invocation -> {
            student.setPassword("replacement-hash");
            return true;
        });
        String body = "{\"passwordChangeToken\":\"grant\",\"studentId\":\"untrusted-id\",\"newPassword\":\"NewTest456\"}";
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(410));
        verify(studentService, never()).findByStudentId(anyString());
        verify(studentService, times(1)).replaceInitialPassword(any(), anyString(), any());
    }

    @Test
    void invalidGrantCannotFallBackToAnUntrustedStudentId() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordChangeToken\":\"invalid\",\"studentId\":\"test-student\",\"newPassword\":\"NewTest456\"}"))
                .andExpect(jsonPath("$.code").value(410));
        verifyNoInteractions(studentService);
    }
}
