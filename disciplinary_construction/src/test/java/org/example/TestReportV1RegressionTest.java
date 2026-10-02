package org.example;

import org.example.controller.*;
import org.example.dto.StudentProfileUpdate;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.example.service.Impl.ActivityServiceImpl;
import org.example.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestReportV1RegressionTest {
    private static final String FILES = "[{\"name\":\"证明.pdf\",\"url\":\"/uploads/fixture.pdf\"}]";
    private final Map<String,Object> user = Map.of("userId", "student-a", "userType", "student");
    private StudentModel student() { StudentModel s = new StudentModel(); s.setId("student-a"); s.setStudentId("2026001"); s.setName("测试学生"); return s; }
    private CurrentUserAccessService access() {
        StudentRepository students = mock(StudentRepository.class);
        when(students.findById("student-a")).thenReturn(Optional.of(student()));
        return new CurrentUserAccessService(students);
    }
    @Test void attachmentsMustBeNonEmptyFilesAndNotExecutableUrls() {
        for (String value : Arrays.asList(null, "", "[]", "null", "{}", "[{\"name\":\"fake\"}]", "[{\"url\":\"javascript:alert(1)\"}]"))
            assertThrows(IllegalArgumentException.class, () -> SubmissionValidation.attachments(value));
        assertDoesNotThrow(() -> SubmissionValidation.attachments(FILES));
    }
    @Test void competitionAndProjectRequireTeamAndInstructor() {
        CompetitionModel c = new CompetitionModel(); c.setAttachments(FILES);
        assertThrows(IllegalArgumentException.class, () -> SubmissionValidation.validate(c));
        c.setTeamMembers("本人"); assertThrows(IllegalArgumentException.class, () -> SubmissionValidation.validate(c));
        c.setInstructorName("导师"); assertDoesNotThrow(() -> SubmissionValidation.validate(c));
        ProjectModel p = new ProjectModel(); p.setAttachments(FILES); p.setTeamMembers("本人"); p.setInstructorName("导师");
        p.setStartDate(LocalDate.of(2026,10,3)); p.setEndDate(LocalDate.of(2026,10,1));
        assertThrows(IllegalArgumentException.class, () -> SubmissionValidation.validate(p));
        p.setEndDate(p.getStartDate()); assertDoesNotThrow(() -> SubmissionValidation.validate(p));
    }
    private DailyTaskModel task(String start, String end) {
        DailyTaskModel t = new DailyTaskModel(); t.setId("task-a"); t.setDeadline(LocalDateTime.now().plusDays(1));
        DailyTaskModel.TaskField a = new DailyTaskModel.TaskField(); a.setFieldName(start); a.setFieldType("date"); a.setRequired(true);
        DailyTaskModel.TaskField b = new DailyTaskModel.TaskField(); b.setFieldName(end); b.setFieldType("date"); b.setRequired(true);
        t.setFields(Arrays.asList(a,b)); return t;
    }
    @Test void returnBeforeDepartureIsRejectedIncludingLegacyTaskDefinitions() {
        DailyTaskModel t = task("离校时间", "返校时间");
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validate(t, "{\"离校时间\":\"2026-10-03\",\"返校时间\":\"2026-10-01\"}"));
        assertDoesNotThrow(() -> TaskSubmissionValidation.validate(t, "{\"离校时间\":\"2026-10-01\",\"返校时间\":\"2026-10-03\"}"));
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validate(t, "{\"离校时间\":\"not-a-date\",\"返校时间\":\"2026-10-03\"}"));
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validate(t, "{}"));
    }
    @Test void customDateDependencyAndInactiveDeadlineAreChecked() {
        DailyTaskModel t = task("出发", "返回"); t.getFields().get(1).setNotBeforeField("出发");
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validate(t, "{\"出发\":\"2026-10-03\",\"返回\":\"2026-10-01\"}"));
        t.setFields(Collections.emptyList()); t.setActive(false);
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validate(t, "内容"));
        t.setActive(true); t.setDeadline(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validate(t, "内容"));
    }
    @Test void hiddenRequiredFieldsDoNotBlockSubmissionAndNumericZeroIsValid() {
        DailyTaskModel t = new DailyTaskModel(); DailyTaskModel.TaskField f = new DailyTaskModel.TaskField();
        f.setFieldName("人数"); f.setFieldType("number"); f.setRequired(true); t.setFields(List.of(f));
        assertDoesNotThrow(() -> TaskSubmissionValidation.validate(t, "{\"人数\":0}"));
        f.setConditionalField("参加"); f.setConditionalValue("是");
        assertDoesNotThrow(() -> TaskSubmissionValidation.validate(t, "{\"参加\":\"否\"}"));
    }
    @Test void categoryIsIndependentFromRegistrationAndDefaultsForLegacyTasks() {
        assertEquals("daily", TaskSubmissionValidation.category(null));
        assertEquals("volunteer", TaskSubmissionValidation.category("volunteer"));
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.category("registration"));
    }
    @Test void courseRequiresRealLinkAndDateButNotRemovedFields() {
        PartyCourseModel c = new PartyCourseModel(); c.setTitle("微党课"); c.setDescription("https://example.edu.cn/article"); c.setCourseDate("2026-10-01");
        assertDoesNotThrow(() -> SubmissionValidation.validate(c));
        c.setDescription("描述文字"); assertThrows(IllegalArgumentException.class, () -> SubmissionValidation.validate(c));
    }
    @Test void profileAcceptsStudentFieldsButExcludesPoliticalStatus() {
        StudentProfileUpdate dto = new StudentProfileUpdate(); dto.setMajor("计算机"); dto.setClassName("一班"); dto.setSupervisor("导师"); dto.setWorkStatus("班委");
        StudentModel result = dto.toStudent("student-a"); assertEquals("计算机",result.getMajor()); assertEquals("一班",result.getClassName());
        assertEquals("导师",result.getSupervisor()); assertEquals("班委",result.getWorkStatus()); assertNull(result.getPoliticalStatus());
    }
    private StudyRecordModel study() {
        StudyRecordModel r = new StudyRecordModel(); r.setCourse("课程"); r.setSemester("2026秋"); r.setScore(85); r.setCredit(2.0); r.setAttachments(FILES); return r;
    }
    @Test void studentStudyRecordUsesAuthenticatedOwnerAndCannotForgeOfficialStatus() {
        StudyRecordRepository repository = mock(StudyRecordRepository.class); when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        StudyRecordController c = new StudyRecordController(repository, access()); StudyRecordModel r = study();
        r.setStudentDbId("victim"); r.setStudentId("victim"); r.setSource("official"); r.setStatus("已通过");
        StudyRecordModel saved = c.save(r,user); assertEquals("student-a", saved.getStudentDbId()); assertEquals("2026001", saved.getStudentId()); assertEquals("student",saved.getSource()); assertEquals("学生自填",saved.getStatus());
    }
    @Test void cannotEditOtherStudentsOrImportedGrades() {
        StudyRecordRepository repository = mock(StudyRecordRepository.class); StudyRecordController c = new StudyRecordController(repository, access());
        StudyRecordModel input = study(); input.setId("record"); StudyRecordModel old = study(); old.setStudentDbId("victim"); old.setSource("student");
        when(repository.findById("record")).thenReturn(Optional.of(old));
        assertThrows(AccessDeniedException.class, () -> c.save(input,user));
        old.setStudentDbId("student-a"); old.setSource(null);
        assertThrows(AccessDeniedException.class, () -> c.save(input,user)); verify(repository, never()).save(any());
        old.setSource("student"); input.setAttachments("[]"); assertThrows(ResponseStatusException.class, () -> c.save(input,user));
    }
    @Test void employmentIntentIsPersistedUnderCurrentStudentNotClientId() {
        EmploymentIntentionRepository repository = mock(EmploymentIntentionRepository.class); when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        EmploymentIntentionController c = new EmploymentIntentionController(repository, access()); EmploymentIntentionModel input = new EmploymentIntentionModel();
        input.setId("victim"); input.setStudentName("伪造"); input.setIntentionType("就业"); input.setTargetCity("武汉");
        EmploymentIntentionModel saved = c.save(input,user); assertEquals("student-a",saved.getId()); assertEquals("2026001",saved.getStudentId()); assertEquals("测试学生",saved.getStudentName());
        verify(repository).save(input);
    }
    @Test void leaveAuditUsesAuthenticatedAdministratorIdentity() {
        LeaveRequestService service = mock(LeaveRequestService.class);
        LeaveRequestController controller = new LeaveRequestController();
        ReflectionTestUtils.setField(controller, "leaveRequestService", service);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access());
        Map<String, Object> admin = Map.of("userId", "admin-a", "userType", "admin", "username", "可信管理员");
        Map<String, Object> body = new HashMap<>(); body.put("auditStatus", "approved"); body.put("auditComment", "通过");
        body.put("auditorId", "forged"); body.put("auditorName", "伪造管理员");
        LeaveRequestModel saved = new LeaveRequestModel(); saved.setId("leave-a");
        when(service.auditLeaveRequest("leave-a", "approved", "通过", "admin-a", "可信管理员")).thenReturn(saved);
        assertEquals(200, controller.auditLeaveRequest("leave-a", body, admin).getBody().get("code"));
        verify(service).auditLeaveRequest("leave-a", "approved", "通过", "admin-a", "可信管理员");
        assertThrows(AccessDeniedException.class, () -> controller.auditLeaveRequest("leave-a", body, user));
    }
    @Test void volunteerSyncUsesStableIdAndSchoolNumberAndDoesNotTreatSignupAsCompletion() {
        MongoTemplate mongo = mock(MongoTemplate.class); VolunteerCompletionService sync = new VolunteerCompletionService(mongo);
        DailyTaskModel nonVolunteer = new DailyTaskModel(); nonVolunteer.setId("task-d"); nonVolunteer.setActivityCategory("daily");
        assertFalse(sync.sync(nonVolunteer, student(), null)); verifyNoInteractions(mongo);
        DailyTaskModel task = new DailyTaskModel(); task.setId("task-a"); task.setActivityCategory("volunteer"); task.setTaskCategory("registration");
        sync.sync(task,student(),null); verifyNoInteractions(mongo);
        ActivityModel activity = new ActivityModel(); activity.setActivityTime(LocalDateTime.now());
        sync.sync(task,student(),activity); sync.sync(task,student(),activity);
        verify(mongo,times(2)).upsert(argThat((Query q) -> "task:task-a:student:student-a".equals(q.getQueryObject().get("_id"))),
                argThat((Update u) -> "2026001".equals(((org.bson.Document) u.getUpdateObject().get("$setOnInsert")).get("studentId"))
                        && "通过".equals(((org.bson.Document) u.getUpdateObject().get("$set")).get("auditStatus"))), eq(VolunteerServiceModel.class));
    }
    @Test void ordinaryVolunteerSubmissionCreatesRecordButRegistrationWaitsForAttendance() {
        MongoTemplate mongo = mock(MongoTemplate.class); VolunteerCompletionService sync = new VolunteerCompletionService(mongo);
        DailyTaskModel ordinary = new DailyTaskModel(); ordinary.setId("task-normal"); ordinary.setActivityCategory("volunteer"); ordinary.setTaskCategory("normal");
        sync.sync(ordinary, student(), null);
        verify(mongo).upsert(any(Query.class), any(Update.class), eq(VolunteerServiceModel.class));

        reset(mongo);
        DailyTaskModel registration = new DailyTaskModel(); registration.setId("task-registration"); registration.setActivityCategory("volunteer"); registration.setTaskCategory("registration");
        sync.sync(registration, student(), null);
        verifyNoInteractions(mongo);
        sync.sync(registration, student(), new ActivityModel());
        verify(mongo).upsert(any(Query.class), any(Update.class), eq(VolunteerServiceModel.class));
    }
    @Test void attendanceSyncAndCompensationReportCreatedSkippedAndOutOfScopeStudents() {
        ActivityRepository activities = mock(ActivityRepository.class);
        DailyTaskRepository tasks = mock(DailyTaskRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        DailyTaskAudienceService audience = mock(DailyTaskAudienceService.class);
        VolunteerCompletionService volunteer = mock(VolunteerCompletionService.class);
        ActivityServiceImpl service = new ActivityServiceImpl();
        ReflectionTestUtils.setField(service, "activityRepository", activities);
        ReflectionTestUtils.setField(service, "taskRepository", tasks);
        ReflectionTestUtils.setField(service, "studentRepository", students);
        ReflectionTestUtils.setField(service, "dailyTaskAudienceService", audience);
        ReflectionTestUtils.setField(service, "volunteerCompletionService", volunteer);
        ActivityModel activity = new ActivityModel(); activity.setId("activity-a"); activity.setTitle("志愿活动"); activity.setTaskId("task-a"); activity.setParticipantStudentIds(Arrays.asList("student-a", "student-b", "student-missing"));
        DailyTaskModel task = new DailyTaskModel(); task.setId("task-a"); task.setActivityCategory("volunteer");
        StudentModel other = new StudentModel(); other.setId("student-b"); other.setStudentId("2026002"); other.setName("范围外学生");
        when(activities.findById("activity-a")).thenReturn(Optional.of(activity));
        when(tasks.findById("task-a")).thenReturn(Optional.of(task));
        when(students.findById("student-a")).thenReturn(Optional.of(student()));
        when(students.findById("student-b")).thenReturn(Optional.of(other));
        when(students.findById("student-missing")).thenReturn(Optional.empty());
        when(audience.matches(eq(task), any(StudentModel.class))).thenAnswer(invocation ->
                "student-a".equals(((StudentModel) invocation.getArgument(1)).getId()));
        when(volunteer.sync(eq(task), any(StudentModel.class), eq(activity))).thenReturn(true, false);

        Map<String, Object> first = service.syncVolunteerAttendance("activity-a");
        assertEquals(1, first.get("syncedCount")); assertEquals(1, first.get("skippedCount")); assertEquals(1, first.get("outOfScopeCount"));
        Map<String, Object> repeated = service.syncVolunteerAttendance("activity-a");
        assertEquals(0, repeated.get("syncedCount")); assertEquals(2, repeated.get("skippedCount"));

        when(activities.findAll()).thenReturn(List.of(activity));
        Map<String, Object> compensated = service.compensateVolunteerAttendance();
        assertEquals(1, compensated.get("activityCount")); assertEquals(0, compensated.get("syncedCount"));
    }
}
