package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.*;
import org.example.dto.StudentProfileUpdate;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.example.service.Impl.*;
import org.example.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityRegressionTest {
    @TempDir Path dir;
    @Test void fileCookieIsReadOnlyAndHttpsCanForceSecureFlag() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.validateToken("fixture-token")).thenReturn(true);
        when(jwt.getUserIdFromToken("fixture-token")).thenReturn("a");
        when(jwt.getUserTypeFromToken("fixture-token")).thenReturn("student");
        when(jwt.getExpirationFromToken("fixture-token")).thenReturn(new Date(System.currentTimeMillis() + 60000));
        org.example.config.JwtAuthenticationFilter filter = new org.example.config.JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        ReflectionTestUtils.setField(filter, "onlineUserService", mock(OnlineUserService.class));
        ReflectionTestUtils.setField(filter, "fileCookieSecure", true);
        try {
            for (String path : List.of("/SCSE@hbut/uploads/fixture.pdf", "/SCSE@hbut/msi/student/profile")) {
                org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", path);
                request.setContextPath("/SCSE@hbut");
                request.setCookies(new javax.servlet.http.Cookie("mis_file_session", "fixture-token"));
                filter.doFilter(request, new org.springframework.mock.web.MockHttpServletResponse(), (req, res) -> {
                    boolean authenticated = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null;
                    assertEquals(path.contains("/uploads/"), authenticated);
                });
            }
            org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/SCSE@hbut/msi/student/profile");
            request.setContextPath("/SCSE@hbut");
            request.addHeader("Authorization", "Bearer fixture-token");
            org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            String cookie = response.getHeader("Set-Cookie");
            assertNotNull(cookie);
            assertTrue(cookie.contains("Secure") && cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"));
            assertTrue(cookie.contains("Path=/SCSE@hbut/uploads"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
    @Test void projectCreationCannotRetainVictimId() {
        ProjectRepository repository = mock(ProjectRepository.class);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProjectServiceImpl service = new ProjectServiceImpl();
        ReflectionTestUtils.setField(service, "projectRepository", repository);
        CurrentUserAccessService access = mock(CurrentUserAccessService.class);
        Map<String,Object> user = Map.of("userId", "a", "userType", "student");
        when(access.requireStudentNumber(user)).thenReturn("student-a");
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "projectService", service);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access);
        ProjectModel input = new ProjectModel(); input.setId("victim-id"); input.setStudentId("victim");
        assertEquals(200, controller.addProject(input, user).getStatusCodeValue());
        verify(repository).save(argThat(p -> p.getId() == null && "student-a".equals(p.getStudentId())));
    }
    @Test void profileDtoCannotCarryCredentialOrInstitutionFields() throws Exception {
        StudentProfileUpdate profile = new ObjectMapper().readValue("{\"id\":\"victim\",\"password\":\"x\",\"status\":\"在读\",\"politicalStatus\":\"中共党员\",\"grade\":\"2026\",\"name\":\"spoof\",\"phone\":\"fixture\"}", StudentProfileUpdate.class);
        StudentModel update = profile.toStudent("a");
        assertEquals("a", update.getId()); assertEquals("fixture", update.getPhone());
        assertNull(update.getPassword()); assertNull(update.getStatus()); assertNull(update.getPoliticalStatus());
        assertNull(update.getGrade()); assertNull(update.getName());
    }
    @Test void privateFileRequiresOwnerAndLegacyFilesAreAdminOnly() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        CurrentUserAccessService access = new CurrentUserAccessService(mock(StudentRepository.class));
        PrivateFileController controller = new PrivateFileController(files, access);
        ReflectionTestUtils.setField(controller, "uploadDir", dir.toString());
        Files.writeString(dir.resolve("fixture.pdf"), "fixture");
        UploadedFileModel meta = new UploadedFileModel(); meta.setOwnerId("a");
        when(files.findById("fixture.pdf")).thenReturn(Optional.of(meta));
        Map<String,Object> owner = Map.of("userId", "a", "userType", "student");
        Map<String,Object> other = Map.of("userId", "b", "userType", "student");
        assertEquals(200, controller.download("fixture.pdf", owner).getStatusCodeValue());
        assertEquals("no-store", controller.download("fixture.pdf", owner).getHeaders().getCacheControl());
        assertThrows(AccessDeniedException.class, () -> controller.download("fixture.pdf", other));
        when(files.findById("fixture.pdf")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> controller.download("fixture.pdf", owner));
        assertEquals(200, controller.download("fixture.pdf", Map.of("userId", "admin", "userType", "admin")).getStatusCodeValue());
        assertEquals(404, controller.download("../fixture.pdf", owner).getStatusCodeValue());
    }
    @Test void tokensAreRevokedByCredentialChangesAndDisabledAccounts() {
        AccountTokenStateService accounts = mock(AccountTokenStateService.class);
        when(accounts.state("a", "admin")).thenReturn("credential-v1");
        JwtUtil jwt = jwt(accounts);
        String token = jwt.generateToken("a", "admin", "fixture");
        assertTrue(jwt.validateToken(token));
        when(accounts.state("a", "admin")).thenReturn("credential-v2");
        assertFalse(jwt.validateToken(token)); assertThrows(IllegalArgumentException.class, () -> jwt.refreshToken(token));
        when(accounts.state("a", "admin")).thenReturn(null);
        assertFalse(jwt.validateToken(token));
    }
    @Test void refreshCannotExtendAbsoluteSession() throws Exception {
        AccountTokenStateService accounts = mock(AccountTokenStateService.class);
        when(accounts.state("a", "student")).thenReturn("fixture");
        JwtUtil jwt = jwt(accounts);
        ReflectionTestUtils.setField(jwt, "absoluteSessionMs", 60000L);
        String first = jwt.generateToken("a", "student", "fixture");
        String refreshed = jwt.refreshToken(first);
        assertEquals(jwt.getExpirationFromToken(first), jwt.getExpirationFromToken(refreshed));
    }
    @Test void accountStateDeniesDeletedDisabledAndUnactivatedUsers() {
        StudentRepository students = mock(StudentRepository.class); AdminRepository admins = mock(AdminRepository.class);
        AccountTokenStateService state = new AccountTokenStateService(students, admins);
        when(students.findById("a")).thenReturn(Optional.empty()); assertNull(state.state("a", "student"));
        StudentModel s = new StudentModel(); s.setPassword("fixture-hash"); s.setPasswordChangeRequired(true);
        when(students.findById("a")).thenReturn(Optional.of(s)); assertNull(state.state("a", "student"));
        AdminModel a = new AdminModel(); a.setPassword("fixture-hash"); a.setIsActive(false);
        when(admins.findById("a")).thenReturn(Optional.of(a)); assertNull(state.state("a", "admin"));
    }
    @Test void fixedInitialPasswordRequiresChangeAndCannotReplacePersonalPassword() {
        StudentRepository students = mock(StudentRepository.class);
        StudentServiceImpl service = new StudentServiceImpl(); ReflectionTestUtils.setField(service, "studentRepository", students);
        StudentModel student = new StudentModel(); student.setStudentId("fixture123456");
        student.setPassword(new BCryptPasswordEncoder().encode("Hbut_123456"));
        when(students.findByStudentId(student.getStudentId())).thenReturn(student);
        assertSame(student, service.login(student.getStudentId(), "Hbut_123456"));
        assertTrue(service.isDefaultPassword(student.getStudentId(), "Hbut_123456"));
        assertFalse(service.changePassword(student.getStudentId(), null, "NewFixture123!"));
        assertFalse(service.changePassword(student.getStudentId(), "WrongPassword123!", "NewFixture123!"));
        verify(students, never()).save(any());
        assertTrue(service.changePassword(student.getStudentId(), "Hbut_123456", "NewFixture123!"));
        assertFalse(student.getPasswordChangeRequired());
        assertFalse(service.isDefaultPassword(student.getStudentId(), "Hbut_123456"));
        assertFalse(service.changePassword(student.getStudentId(), "Hbut_123456", "AnotherFixture123!"));
        assertFalse(service.changePassword(student.getStudentId(), "NewFixture123!", "Hbut_123456"));
    }
    @Test void administratorResetRestoresFixedFormatAndCreateDefaultsToIt() {
        StudentRepository students = mock(StudentRepository.class);
        StudentServiceImpl service = new StudentServiceImpl(); ReflectionTestUtils.setField(service, "studentRepository", students);
        StudentModel student = new StudentModel(); student.setStudentId("2026123456"); student.setGrade("2026级");
        when(students.save(any())).thenAnswer(i -> i.getArgument(0));
        service.createStudent(student);
        assertTrue(new BCryptPasswordEncoder().matches("Hbut_123456", student.getPassword()));
        assertTrue(student.getPasswordChangeRequired()); assertNull(student.getInitialPasswordExpiresAt());
        when(students.findByStudentId(student.getStudentId())).thenReturn(student);
        assertTrue(service.resetStudentPassword(student.getStudentId(), null));
        assertTrue(service.isDefaultPassword(student.getStudentId(), "Hbut_123456"));
        assertSame(student, service.login(student.getStudentId(), "Hbut_123456"));
    }
    @Test void initialLoginDoesNotIssueBusinessTokenEvenForLegacyAccount() {
        StudentRepository students = mock(StudentRepository.class);
        StudentServiceImpl service = new StudentServiceImpl(); ReflectionTestUtils.setField(service, "studentRepository", students);
        StudentModel student = new StudentModel(); student.setStudentId("2026123456"); student.setName("Fixture");
        student.setPassword(new BCryptPasswordEncoder().encode("Hbut_123456"));
        when(students.findByStudentId(student.getStudentId())).thenReturn(student);
        StudentController controller = new StudentController();
        JwtUtil jwt = mock(JwtUtil.class);
        ReflectionTestUtils.setField(controller, "studentService", service);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwt);
        ReflectionTestUtils.setField(controller, "loginAttemptLimiter", new LoginAttemptLimiter());
        Map<String,Object> response = controller.login(Map.of("account", student.getStudentId(), "password", "Hbut_123456")).getBody();
        assertEquals(true, response.get("requirePasswordChange"));
        assertNull(response.get("data"));
        verifyNoInteractions(jwt);
    }
    @Test void temporaryPasswordExpiresAndMustBeChanged() {
        StudentRepository students = mock(StudentRepository.class);
        StudentServiceImpl service = new StudentServiceImpl(); ReflectionTestUtils.setField(service, "studentRepository", students);
        StudentModel s = new StudentModel(); s.setStudentId("fixture");
        String temporary = PasswordPolicy.temporaryPassword(); s.setPassword(new BCryptPasswordEncoder().encode(temporary));
        s.setPasswordChangeRequired(true); s.setInitialPasswordExpiresAt(new Date(System.currentTimeMillis() + 60000));
        when(students.findByStudentId("fixture")).thenReturn(s);
        assertTrue(service.isDefaultPassword("fixture", temporary));
        s.setInitialPasswordExpiresAt(new Date(0)); assertFalse(service.isDefaultPassword("fixture", temporary));
        assertNull(service.login("fixture", temporary));
    }
    @Test void accountRateLimitIsEnforced() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < 10; i++) limiter.check("student", "fixture");
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> limiter.check("student", "fixture"));
        assertDoesNotThrow(() -> limiter.check("student", "other"));
    }
    @Test void unauditedPartyStageCannotGrantTaskEligibility() {
        PartyApplicationService applications = mock(PartyApplicationService.class);
        PartyApplicationModel application = new PartyApplicationModel(); application.setCurrentStage("中共党员"); application.setAuditStatus("待审核");
        when(applications.getByStudentId("fixture")).thenReturn(application);
        DailyTaskAudienceService audience = new DailyTaskAudienceService(); ReflectionTestUtils.setField(audience, "partyApplicationService", applications);
        StudentModel student = new StudentModel(); student.setStudentId("fixture");
        assertNull(audience.resolvePartyStage(student));
        application.setAuditStatus("通过"); assertEquals("中共党员", audience.resolvePartyStage(student));
    }
    private JwtUtil jwt(AccountTokenStateService accounts) {
        JwtUtil jwt = new JwtUtil(); ReflectionTestUtils.setField(jwt, "accountTokenStateService", accounts);
        ReflectionTestUtils.setField(jwt, "secretKey", "test-only-fixture-secret-key-longer-than-32-bytes");
        ReflectionTestUtils.setField(jwt, "expirationTime", 86400000L); jwt.validateConfiguration(); return jwt;
    }
}
