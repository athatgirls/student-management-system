package org.example;

import org.example.aspect.CurrentUserAspect;
import org.example.controller.StudentController;
import org.example.dto.StudentProfileUpdate;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentService;
import org.example.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentProfileControllerTest {
    private static final String URL = "/msi/student/profile/update";
    private final StudentService students = mock(StudentService.class);
    private StudentController controller;
    private MockMvc mvc;

    @BeforeEach void setup() {
        StudentRepository repository = mock(StudentRepository.class);
        StudentModel student = new StudentModel(); student.setId("student-a");
        when(repository.findById("student-a")).thenReturn(Optional.of(student));
        controller = new StudentController();
        ReflectionTestUtils.setField(controller, "studentService", students);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", new CurrentUserAccessService(repository));
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.validateToken("fixture-token")).thenReturn(true);
        when(jwt.getUserIdFromToken("fixture-token")).thenReturn("student-a");
        when(jwt.getUserTypeFromToken("fixture-token")).thenReturn("student");
        CurrentUserAspect resolver = new CurrentUserAspect();
        ReflectionTestUtils.setField(resolver, "jwtUtil", jwt);
        mvc = MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(resolver).build();
    }

    @Test void postAndLegacyPutSaveOnlyAllowedFieldsForAuthenticatedStudent() throws Exception {
        when(students.updateStudent(any())).thenAnswer(i -> i.getArgument(0));
        String body = "{\"id\":\"victim\",\"phone\":\"fixture-phone\",\"birthDate\":\"2001-01-02T00:00:00.000Z\","
                + "\"password\":\"injected-password\",\"politicalStatus\":\"发展对象\",\"major\":\"计算机\",\"className\":\"一班\",\"supervisor\":\"导师\"}";
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT}) {
            mvc.perform(request(method, URL).header("Authorization", "Bearer fixture-token")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value("student-a"))
                    .andExpect(jsonPath("$.data.phone").value("fixture-phone"));
        }
        verify(students, times(2)).updateStudent(argThat(s -> "student-a".equals(s.getId())
                && s.getPassword() == null && "发展对象".equals(s.getPoliticalStatus()) && "计算机".equals(s.getMajor())
                && "一班".equals(s.getClassName()) && "导师".equals(s.getSupervisor()) && s.getBirthDate() != null));
    }

    @Test void getCannotSaveProfile() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(students);
    }

    @Test void deletedRecordDoesNotReturnFalseSuccess() throws Exception {
        mvc.perform(post(URL).header("Authorization", "Bearer fixture-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"fixture\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
    }

    @Test void missingIdentityOrAdminCannotUseStudentSelfService() {
        assertThrows(AccessDeniedException.class, () -> controller.updateUserProfile(new StudentProfileUpdate(), null));
        assertThrows(AccessDeniedException.class, () -> controller.updateUserProfile(new StudentProfileUpdate(),
                Map.of("userId", "admin", "userType", "admin")));
        verifyNoInteractions(students);
    }
}
