package org.example;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.example.service.Impl.DailyTaskServiceImpl;
import org.example.service.Impl.LeaveRequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudentIssue11RegressionTest {
    private static final String PROOF = "[{\"url\":\"/SCSE@hbut/uploads/proof.pdf\",\"name\":\"证明.pdf\"}]";

    @Test void s2InvalidLeaveIsABusinessValidationErrorNotAnInternalServerError() {
        org.example.controller.LeaveRequestController controller = new org.example.controller.LeaveRequestController();
        LeaveRequestService service = mock(LeaveRequestService.class);
        CurrentUserAccessService access = mock(CurrentUserAccessService.class);
        Map<String,Object> user = Map.of("userId", "student-a", "userType", "student");
        when(access.requireStudentNumber(user)).thenReturn("2026001");
        when(service.createLeaveRequest(any())).thenThrow(new IllegalArgumentException("请假开始日期不能早于今天"));
        ReflectionTestUtils.setField(controller, "leaveRequestService", service);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access);
        LeaveRequestModel forged = new LeaveRequestModel(); forged.setStudentId("other-student");
        Map<String,Object> response = controller.createLeaveRequest(forged, user).getBody();
        assertEquals(400, response.get("code"));
        assertEquals("请假开始日期不能早于今天", response.get("msg"));
        assertEquals("2026001", forged.getStudentId());
    }

    @Test void s2PastAndMissingLeaveDatesFailBeforeAnyDatabaseWrite() {
        LeaveRequestServiceImpl service = new LeaveRequestServiceImpl();
        LeaveRequestRepository repository = mock(LeaveRequestRepository.class);
        StudentService students = mock(StudentService.class);
        ReflectionTestUtils.setField(service, "leaveRequestRepository", repository);
        ReflectionTestUtils.setField(service, "studentService", students);
        LeaveRequestModel leave = new LeaveRequestModel();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        leave.setStartDate(today.minusDays(1)); leave.setEndDate(today.plusDays(1));
        assertEquals("请假开始日期不能早于今天", assertThrows(IllegalArgumentException.class, () -> service.createLeaveRequest(leave)).getMessage());
        leave.setStartDate(null);
        assertThrows(IllegalArgumentException.class, () -> service.createLeaveRequest(leave));
        leave.setStartDate(today); leave.setEndDate(null);
        assertThrows(IllegalArgumentException.class, () -> service.createLeaveRequest(leave));
        verifyNoInteractions(repository, students);
    }

    @Test void s2TodayAndFutureLeaveDatesAreAcceptedAndReversedDatesRejected() {
        LeaveRequestServiceImpl service = new LeaveRequestServiceImpl();
        LeaveRequestRepository repository = mock(LeaveRequestRepository.class);
        StudentService students = mock(StudentService.class);
        ReflectionTestUtils.setField(service, "leaveRequestRepository", repository);
        ReflectionTestUtils.setField(service, "studentService", students);
        StudentModel student = new StudentModel(); student.setName("学生"); student.setGrade("2026");
        student.setMajor("计算机"); student.setClassName("一班"); student.setPhone("13800000000");
        when(students.findByStudentId("2026001")).thenReturn(student);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        LeaveRequestModel leave = new LeaveRequestModel(); leave.setStudentId("2026001");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        leave.setStartDate(today); leave.setEndDate(today);
        assertEquals(1, service.createLeaveRequest(leave).getDays());
        leave.setStartDate(today.plusDays(1)); leave.setEndDate(today.plusDays(2));
        assertEquals(2, service.createLeaveRequest(leave).getDays());
        leave.setEndDate(today);
        assertThrows(IllegalArgumentException.class, () -> service.createLeaveRequest(leave));
        verify(repository, times(2)).save(any());
    }

    @Test void s2HistoricalLeaveRecordsRemainEditableByExistingAdministrativeWorkflow() {
        LeaveRequestServiceImpl service = new LeaveRequestServiceImpl();
        LeaveRequestRepository repository = mock(LeaveRequestRepository.class);
        ReflectionTestUtils.setField(service, "leaveRequestRepository", repository);
        LeaveRequestModel leave = new LeaveRequestModel();
        leave.setStartDate(LocalDate.of(2020, 1, 1)); leave.setEndDate(LocalDate.of(2020, 1, 2));
        assertDoesNotThrow(() -> service.updateLeaveRequest(leave));
        verify(repository).save(leave);
    }

    private TaskAttachmentAccessService attachmentService(UploadedFileRepository files) {
        return new TaskAttachmentAccessService(mock(DailyTaskRepository.class), files,
                mock(CurrentUserAccessService.class), mock(DailyTaskAudienceService.class));
    }
    private DailyTaskSubmissionModel submission(String attachments) {
        DailyTaskSubmissionModel s = new DailyTaskSubmissionModel();
        s.setTaskId("task"); s.setStudentId("student-a"); s.setContent("完成说明"); s.setAttachments(attachments);
        return s;
    }

    @Test void s4SubmissionProofsMustBelongToTheAuthenticatedStudent() {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        UploadedFileModel file = new UploadedFileModel(); file.setOwnerId("student-a");
        when(files.findById("proof.pdf")).thenReturn(Optional.of(file));
        TaskAttachmentAccessService service = attachmentService(files);
        assertDoesNotThrow(() -> service.validateSubmission(submission(PROOF)));
        file.setOwnerId("other-student");
        assertThrows(IllegalArgumentException.class, () -> service.validateSubmission(submission(PROOF)));
        when(files.findById("proof.pdf")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.validateSubmission(submission(PROOF)));
    }

    @Test void s4InvalidExternalTraversalDuplicateAndOversizedProofListsAreRejected() {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        UploadedFileModel file = new UploadedFileModel(); file.setOwnerId("student-a");
        when(files.findById("proof.pdf")).thenReturn(Optional.of(file));
        TaskAttachmentAccessService service = attachmentService(files);
        for (String value : List.of("{}", "null", "not-json", "[{}]", "[{\"url\":\"https://evil.example/f.pdf\"}]",
                "[{\"url\":\"/SCSE@hbut/uploads/../f.pdf\"}]", "[{\"url\":\"javascript:alert(1)\"}]",
                "[{\"url\":\"/SCSE@hbut/uploads/proof.pdf\"},{\"url\":\"/SCSE@hbut/uploads/proof.pdf\"}]",
                "[{}, {}, {}, {}, {}, {}]"))
            assertThrows(IllegalArgumentException.class, () -> service.validateSubmission(submission(value)), value);
        for (String value : Arrays.asList(null, "", "[]")) assertDoesNotThrow(() -> service.validateSubmission(submission(value)));
    }

    @Test void s4ProofsPersistWithTaskTextAndSurviveLegacyClientResubmission() {
        DailyTaskRepository tasks = mock(DailyTaskRepository.class);
        DailyTaskSubmissionRepository submissions = mock(DailyTaskSubmissionRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        DailyTaskAudienceService audience = mock(DailyTaskAudienceService.class);
        DailyTaskServiceImpl service = new DailyTaskServiceImpl();
        ReflectionTestUtils.setField(service, "taskRepository", tasks);
        ReflectionTestUtils.setField(service, "submissionRepository", submissions);
        ReflectionTestUtils.setField(service, "studentRepository", students);
        ReflectionTestUtils.setField(service, "dailyTaskAudienceService", audience);
        ReflectionTestUtils.setField(service, "taskAttachmentAccessService", attachmentService(files));
        ReflectionTestUtils.setField(service, "volunteerCompletionService", mock(VolunteerCompletionService.class));
        DailyTaskModel task = new DailyTaskModel(); task.setId("task");
        StudentModel student = new StudentModel(); student.setId("student-a");
        UploadedFileModel file = new UploadedFileModel(); file.setOwnerId("student-a");
        when(files.findById("proof.pdf")).thenReturn(Optional.of(file));
        when(tasks.findById("task")).thenReturn(Optional.of(task));
        when(students.findById("student-a")).thenReturn(Optional.of(student));
        when(audience.matches(task, student)).thenReturn(true);
        when(submissions.findByTaskIdAndStudentId("task", "student-a")).thenReturn(Optional.empty());
        when(submissions.save(any())).thenAnswer(i -> { DailyTaskSubmissionModel s = i.getArgument(0); s.setId("saved"); return s; });
        DailyTaskSubmissionModel saved = service.submitTask(submission(PROOF));
        assertEquals(PROOF, saved.getAttachments()); assertEquals("完成说明", saved.getContent());
        when(submissions.findByTaskIdAndStudentId("task", "student-a")).thenReturn(Optional.of(saved));
        assertEquals(PROOF, service.getSubmissionByTaskAndStudent("task", "student-a").getAttachments());
        DailyTaskSubmissionModel legacy = service.submitTask(submission(null));
        assertEquals("saved", legacy.getId()); assertEquals(PROOF, legacy.getAttachments());
        // Explicitly removing attachments is supported, unlike an omitted legacy field.
        assertEquals("[]", service.submitTask(submission("[]")).getAttachments());
        file.setOwnerId("someone-else");
        assertThrows(IllegalArgumentException.class, () -> service.submitTask(submission(PROOF)));
        verify(submissions, times(3)).save(any());
    }
}
