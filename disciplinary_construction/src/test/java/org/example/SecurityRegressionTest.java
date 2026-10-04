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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
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
        input.setAttachments("[{\"url\":\"/uploads/fixture.pdf\"}]"); input.setTeamMembers("本人"); input.setInstructorName("导师");
        assertEquals(200, controller.addProject(input, user).getStatusCodeValue());
        verify(repository).save(argThat(p -> p.getId() == null && "student-a".equals(p.getStudentId())));
    }
    @Test void profileDtoCannotCarryCredentialOrInstitutionControlledFields() throws Exception {
        StudentProfileUpdate profile = new ObjectMapper().readValue("{\"id\":\"victim\",\"password\":\"x\",\"status\":\"在读\",\"politicalStatus\":\"中共党员\",\"grade\":\"2026\",\"name\":\"spoof\",\"phone\":\"fixture\",\"workStatus\":\"班委\"}", StudentProfileUpdate.class);
        StudentModel update = profile.toStudent("a");
        assertEquals("a", update.getId()); assertEquals("fixture", update.getPhone()); assertEquals("班委", update.getWorkStatus());
        assertNull(update.getPassword()); assertNull(update.getStatus()); assertNull(update.getPoliticalStatus());
        assertNull(update.getGrade()); assertNull(update.getName());
    }
    @Test void privateFileRequiresOwnerAndLegacyFilesAreAdminOnly() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        CurrentUserAccessService access = new CurrentUserAccessService(mock(StudentRepository.class));
        PrivateFileController controller = new PrivateFileController(files, access, mock(TaskAttachmentAccessService.class));
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
    @Test void majorWritesRequireAdminAndMajorListRequiresAuthentication() throws Exception {
        FilterChainProxy security = securityFilterChain();
        List<String[]> adminOnlyEndpoints = List.of(
                new String[]{"POST", "/msi/major/create"},
                new String[]{"DELETE", "/msi/major/delete-by-name"},
                new String[]{"DELETE", "/msi/major/delete/software-engineering"},
                new String[]{"PUT", "/msi/student/batch-update-major"},
                new String[]{"POST", "/msi/student/batch-update-major"});

        for (String[] endpoint : adminOnlyEndpoints) {
            assertEquals(401, status(security, null, endpoint[0], endpoint[1]));
            assertEquals(403, status(security, "student-token", endpoint[0], endpoint[1]));
            assertEquals(204, status(security, "admin-token", endpoint[0], endpoint[1]));
        }

        assertEquals(401, status(security, null, "GET", "/msi/major/list"));
        assertEquals(204, status(security, "student-token", "GET", "/msi/major/list"));
    }

    @Test void reportFixesPreserveAdministratorAuthorizationOnPostAliases() throws Exception {
        FilterChainProxy security = securityFilterChain();
        for (String url : List.of("/msi/activities/delete/fixture", "/msi/daily-tasks/delete/fixture",
                "/msi/competitions/admin/fixture/audit", "/msi/papers/admin/fixture/audit",
                "/msi/patents/admin/fixture/audit", "/msi/projects/admin/fixture/audit", "/msi/thought-report/audit/fixture",
                "/msi/leave/audit/fixture")) {
            assertEquals(401, status(security,null,"POST",url));
            assertEquals(403, status(security,"student-token","POST",url));
            assertEquals(204, status(security,"admin-token","POST",url));
        }
        assertEquals(403,status(security,"student-token","GET","/msi/employment-intentions/admin/list"));
        assertEquals(204,status(security,"admin-token","GET","/msi/employment-intentions/admin/list"));
        assertEquals(204,status(security,"student-token","POST","/msi/honors/create"));
        assertEquals(403,status(security,"student-token","POST","/msi/honors/audit"));
        for (String url : List.of("/msi/honors/fixture", "/msi/honors/fixture/update", "/msi/honors/fixture/delete")) {
            String method = url.endsWith("fixture") ? "GET" : "POST";
            assertEquals(401, status(security, null, method, url));
            assertEquals(204, status(security, "student-token", method, url));
            assertEquals(204, status(security, "admin-token", method, url));
        }
    }

    private FilterChainProxy securityFilterChain() throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.validateToken(anyString())).thenReturn(true);
        when(jwt.getUserIdFromToken(anyString())).thenReturn("fixture-user");
        when(jwt.getUsernameFromToken(anyString())).thenReturn("fixture-user");
        when(jwt.getExpirationFromToken(anyString())).thenReturn(new Date(System.currentTimeMillis() + 60000));
        when(jwt.getUserTypeFromToken("student-token")).thenReturn("student");
        when(jwt.getUserTypeFromToken("admin-token")).thenReturn("admin");

        org.example.config.JwtAuthenticationFilter jwtFilter = new org.example.config.JwtAuthenticationFilter();
        ReflectionTestUtils.setField(jwtFilter, "jwtUtil", jwt);
        ReflectionTestUtils.setField(jwtFilter, "onlineUserService", mock(OnlineUserService.class));
        org.example.config.SecurityConfig config = new org.example.config.SecurityConfig();
        ReflectionTestUtils.setField(config, "jwtAuthenticationFilter", jwtFilter);

        ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<Object>() {
            @Override public <T> T postProcess(T object) { return object; }
        };
        AuthenticationManager testAuthenticationManager = authentication -> authentication;
        AuthenticationManagerBuilder authenticationManager = new AuthenticationManagerBuilder(postProcessor);
        authenticationManager.parentAuthenticationManager(testAuthenticationManager);
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class);
        applicationContext.refresh();
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, applicationContext);
        sharedObjects.put(AuthenticationManager.class, testAuthenticationManager);
        HttpSecurity http = new HttpSecurity(postProcessor, authenticationManager, sharedObjects);
        SecurityFilterChain chain = config.filterChain(http);
        return new FilterChainProxy(chain);
    }

    private int status(FilterChainProxy security, String token, String method, String path) throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        security.doFilter(request, response, (req, res) -> ((javax.servlet.http.HttpServletResponse) res).setStatus(204));
        return response.getStatus();
    }
    private JwtUtil jwt(AccountTokenStateService accounts) {
        JwtUtil jwt = new JwtUtil(); ReflectionTestUtils.setField(jwt, "accountTokenStateService", accounts);
        ReflectionTestUtils.setField(jwt, "secretKey", "test-only-fixture-secret-key-longer-than-32-bytes");
        ReflectionTestUtils.setField(jwt, "expirationTime", 86400000L); jwt.validateConfiguration(); return jwt;
    }
}
