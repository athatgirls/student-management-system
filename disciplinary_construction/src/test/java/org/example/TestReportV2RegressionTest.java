package org.example;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.config.TimestampSerializationConfiguration;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.example.service.Impl.DailyTaskServiceImpl;
import org.example.util.UtcTimestamps;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestReportV2RegressionTest {
    @Test void taskCutoffUsesBeijingWallTimeRegardlessOfServerTimezone() {
        DailyTaskModel task = new DailyTaskModel();
        task.setDeadline(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusMinutes(1));
        assertThrows(IllegalArgumentException.class, () -> org.example.util.TaskSubmissionValidation.validate(task, "content"));
        task.setDeadline(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).plusMinutes(1));
        assertDoesNotThrow(() -> org.example.util.TaskSubmissionValidation.validate(task, "content"));
    }
    @Test void utcEventTimesCarryAnOffsetButLocalDeadlineIsNotShifted() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .registerModule(new TimestampSerializationConfiguration().utcEventTimestamps())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        DailyTaskModel task = new DailyTaskModel();
        task.setCreateTime(LocalDateTime.of(2026, 10, 1, 7, 36));
        task.setDeadline(LocalDateTime.of(2026, 10, 1, 15, 36));
        JsonNode json = mapper.readTree(mapper.writeValueAsString(task));
        assertEquals("2026-10-01T07:36:00Z", json.get("createTime").asText());
        assertEquals("2026-10-01T15:36:00", json.get("deadline").asText());
        assertEquals(task.getCreateTime(), mapper.readValue(mapper.writeValueAsString(task), DailyTaskModel.class).getCreateTime());
        assertNull(UtcTimestamps.toWire(null));
    }

    @Test void publishedFilesAreReadableOnlyToTaskRecipientsAndOwner() {
        DailyTaskRepository tasks = mock(DailyTaskRepository.class);
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        StudentModel student = new StudentModel(); student.setId("s1"); student.setGrade("2026");
        when(students.findById("s1")).thenReturn(Optional.of(student));
        DailyTaskAudienceService audience = new DailyTaskAudienceService();
        ReflectionTestUtils.setField(audience, "partyApplicationService", mock(PartyApplicationService.class));
        TaskAttachmentAccessService service = new TaskAttachmentAccessService(tasks, files, new CurrentUserAccessService(students), audience);
        UploadedFileModel file = new UploadedFileModel(); file.setOwnerId("admin-a");
        when(files.findById("handout.pdf")).thenReturn(Optional.of(file));
        DailyTaskModel task = new DailyTaskModel(); task.setCreatorId("admin-a");
        task.setAttachments(List.of("/SCSE@hbut/uploads/handout.pdf")); task.setAllowedGrades(List.of("2026"));
        assertDoesNotThrow(() -> service.validate(task));
        when(tasks.findByActiveAndAttachmentsContaining(true, "/SCSE@hbut/uploads/handout.pdf")).thenReturn(List.of(task));
        Map<String,Object> user = Map.of("userId", "s1", "userType", "student");
        assertTrue(service.canDownload("handout.pdf", file, user));
        student.setGrade("2025"); assertFalse(service.canDownload("handout.pdf", file, user));
        file.setOwnerId("other-student"); assertFalse(service.canDownload("handout.pdf", file, user));
        assertThrows(IllegalArgumentException.class, () -> service.validate(task));
        assertFalse(service.canDownload("handout.pdf", null, user));
    }

    @Test void invalidAndOversizedTaskAttachmentListsAreRejected() {
        TaskAttachmentAccessService service = new TaskAttachmentAccessService(mock(DailyTaskRepository.class),
                mock(UploadedFileRepository.class), mock(CurrentUserAccessService.class), mock(DailyTaskAudienceService.class));
        DailyTaskModel task = new DailyTaskModel();
        for (String url : Arrays.asList("javascript:alert(1)", "https://evil.example/f.pdf", "/SCSE@hbut/uploads/../f.pdf", null)) {
            task.setAttachments(Collections.singletonList(url));
            assertThrows(IllegalArgumentException.class, () -> service.validate(task));
        }
        task.setAttachments(Collections.nCopies(6, "/SCSE@hbut/uploads/f.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(task));
    }

    @Test void ownCompletionUsesOnlyOwnSubmissionsAndExcludesRegistrationsAndOutOfScopeTasks() {
        StudentRepository students = mock(StudentRepository.class);
        StudentModel student = new StudentModel(); student.setId("s1"); student.setStudentId("2026001");
        when(students.findById("s1")).thenReturn(Optional.of(student));
        DailyTaskRepository tasks = mock(DailyTaskRepository.class);
        DailyTaskModel normal = new DailyTaskModel(); normal.setId("normal");
        DailyTaskModel restricted = new DailyTaskModel(); restricted.setId("restricted");
        DailyTaskModel signup = new DailyTaskModel(); signup.setTaskCategory("registration");
        when(tasks.findByActive(true)).thenReturn(List.of(normal, restricted, signup));
        DailyTaskSubmissionRepository submissions = mock(DailyTaskSubmissionRepository.class);
        DailyTaskSubmissionModel submitted = new DailyTaskSubmissionModel(); submitted.setTaskId("normal");
        when(submissions.findByStudentId("s1")).thenReturn(List.of(submitted));
        DailyTaskAudienceService audience = mock(DailyTaskAudienceService.class);
        when(audience.matches(eq(normal), eq(student), any())).thenReturn(true);
        DailyTaskServiceImpl service = new DailyTaskServiceImpl();
        ReflectionTestUtils.setField(service, "studentRepository", students);
        ReflectionTestUtils.setField(service, "taskRepository", tasks);
        ReflectionTestUtils.setField(service, "submissionRepository", submissions);
        ReflectionTestUtils.setField(service, "dailyTaskAudienceService", audience);
        Map<String,Object> result = service.getStudentTaskCompletionAnalysis("s1");
        assertEquals(1, result.get("totalRequired")); assertEquals(1, result.get("completedCount"));
        assertEquals(100.0, result.get("completionRate"));
        verify(students, never()).findAll(); verify(submissions, never()).findAll();
    }
}
