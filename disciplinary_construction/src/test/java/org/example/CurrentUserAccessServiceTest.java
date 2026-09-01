package org.example;

import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.service.CurrentUserAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserAccessServiceTest {

    private CurrentUserAccessService accessService;
    private Map<String, Object> studentUser;

    @BeforeEach
    void setUp() {
        StudentRepository repository = mock(StudentRepository.class);
        StudentModel student = new StudentModel();
        student.setId("database-id");
        student.setStudentId("10240001");
        when(repository.findById("database-id")).thenReturn(Optional.of(student));
        accessService = new CurrentUserAccessService(repository);

        studentUser = new HashMap<>();
        studentUser.put("userId", "database-id");
        studentUser.put("userType", "student");
    }

    @Test
    void studentCanAccessOwnDatabaseIdAndStudentNumber() {
        assertDoesNotThrow(() -> accessService.requireStudentAccess(studentUser, "database-id"));
        assertDoesNotThrow(() -> accessService.requireStudentAccess(studentUser, "10240001"));
        assertEquals("10240001", accessService.requireStudentNumber(studentUser));
    }

    @Test
    void studentCannotAccessAnotherStudent() {
        assertThrows(AccessDeniedException.class,
                () -> accessService.requireStudentAccess(studentUser, "10249999"));
    }

    @Test
    void administratorCanAccessStudentScopedRecords() {
        Map<String, Object> adminUser = new HashMap<>();
        adminUser.put("userId", "admin-id");
        adminUser.put("userType", "admin");
        assertDoesNotThrow(() -> accessService.requireStudentAccess(adminUser, "any-student"));
    }
}
