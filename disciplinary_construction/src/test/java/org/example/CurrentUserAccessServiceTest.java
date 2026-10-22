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
    void studentAccessesOwnRecordsByAccountIdAndStudentIdIsDisplayOnly() {
        assertDoesNotThrow(() -> accessService.requireStudentRecord(studentUser, "database-id"));
        // 学号仅展示/导入匹配，禁止作为关联键匹配（见 org.example.dto.StudentRef）
        assertThrows(AccessDeniedException.class, () -> accessService.requireStudentRecord(studentUser, "10240001"));
        assertEquals("10240001", accessService.requireCurrentStudentId(studentUser));
    }

    @Test
    void studentCannotAccessAnotherStudent() {
        assertThrows(AccessDeniedException.class,
                () -> accessService.requireStudentRecord(studentUser, "10249999"));
    }

    @Test
    void administratorCanAccessStudentScopedRecords() {
        Map<String, Object> adminUser = new HashMap<>();
        adminUser.put("userId", "admin-id");
        adminUser.put("userType", "admin");
        assertDoesNotThrow(() -> accessService.requireStudentRecord(adminUser, "any-student"));
    }
}
