package org.example;

import org.example.dto.StudentRef;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    private AppointmentRepository repository;
    private AppointmentService service;
    private StudentRefService refs;
    private final Map<String, Object> user = Map.of("userId", "db-a", "userType", "student");
    @BeforeEach void setup() {
        StudentRepository students = mock(StudentRepository.class);
        StudentModel student = new StudentModel(); student.setId("db-a"); student.setStudentId("1001"); student.setName("学生甲");
        when(students.findById("db-a")).thenReturn(Optional.of(student));
        repository = mock(AppointmentRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        refs = mock(StudentRefService.class);
        when(refs.current(user)).thenReturn(new StudentRef("db-a", "1001", "学生甲"));
        service = new AppointmentService(repository, new CurrentUserAccessService(students), refs, mock(UploadedFileRepository.class));
    }
    private AppointmentModel input() {
        AppointmentModel record = new AppointmentModel(); record.setPosition("班长"); record.setOrganization("一班"); record.setStartDate(LocalDate.of(2026, 9, 1));
        return record;
    }
    @Test void bindsIdentityFromSessionAndIgnoresSubmittedIdentity() {
        AppointmentModel input = input(); input.setId("forged"); input.setStudentId("other"); input.setStudentAccountId("other-db");
        AppointmentModel saved = service.save(null, input, user);
        assertEquals("db-a", saved.getStudentAccountId()); assertEquals("1001", saved.getStudentId()); assertNull(saved.getId()); assertNotNull(saved.getCreateTime());
    }
    @Test void deniesUpdateAndDeleteOfAnotherStudentRecord() {
        AppointmentModel existing = input(); existing.setId("record-b"); existing.setStudentAccountId("db-b");
        when(repository.findById("record-b")).thenReturn(Optional.of(existing));
        assertThrows(AccessDeniedException.class, () -> service.save("record-b", input(), user));
        assertThrows(AccessDeniedException.class, () -> service.delete("record-b", user));
        verify(repository, never()).save(any()); verify(repository, never()).delete(any(AppointmentModel.class));
    }
    @Test void rejectsReversedDatesAndUnownedAttachments() {
        AppointmentModel input = input(); input.setEndDate(LocalDate.of(2026, 8, 1));
        assertThrows(ResponseStatusException.class, () -> service.save(null, input, user));
        input.setEndDate(null); input.setAttachments("[{\"url\":\"/SCSE@hbut/uploads/other.pdf\"}]");
        assertThrows(ResponseStatusException.class, () -> service.save(null, input, user));
        verify(repository, never()).save(any());
    }
    @Test void administratorCannotMutateStudentRecord() {
        when(refs.current(any())).thenThrow(new AccessDeniedException("当前操作仅限学生账号"));
        assertThrows(AccessDeniedException.class, () -> service.save(null, input(), Map.of("userId", "admin", "userType", "admin")));
    }
}
