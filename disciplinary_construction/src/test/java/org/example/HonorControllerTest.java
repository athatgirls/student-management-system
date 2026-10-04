package org.example;

import org.example.controller.HonorController;
import org.example.model.HonorModel;
import org.example.service.CurrentUserAccessService;
import org.example.service.HonorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HonorControllerTest {
    private final HonorService honors = mock(HonorService.class);
    private final CurrentUserAccessService access = mock(CurrentUserAccessService.class);
    private HonorController controller;
    private final Map<String, Object> student = Map.of("userId", "student-a", "userType", "student");
    private final Map<String, Object> admin = Map.of("userId", "admin-a", "userType", "admin");

    @BeforeEach
    void setUp() {
        controller = new HonorController();
        ReflectionTestUtils.setField(controller, "service", honors);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access);
    }

    @Test
    void studentCanReadAndUpdateOwnHonorWithoutChangingOwnership() {
        HonorModel existing = existingHonor();
        HonorModel update = new HonorModel();
        update.setUserId("victim");
        update.setEvidenceUrl("/uploads/new-proof.pdf");
        when(honors.findById("honor-a")).thenReturn(existing);
        when(honors.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(200, controller.detail("honor-a", student).getStatusCodeValue());
        assertEquals(200, controller.update("honor-a", update, student).getStatusCodeValue());

        verify(access, times(2)).requireStudentAccess(student, "student-a");
        verify(honors).update(argThat(item -> "honor-a".equals(item.getId())
                && "student-a".equals(item.getUserId())
                && existing.getCreateTime().equals(item.getCreateTime())));
    }

    @Test
    void administratorCanDeleteAnyStudentsHonor() {
        when(honors.findById("honor-a")).thenReturn(existingHonor());

        assertEquals(200, controller.delete("honor-a", admin).getStatusCodeValue());

        verify(access).requireStudentAccess(admin, "student-a");
        verify(honors).delete("honor-a");
    }

    private HonorModel existingHonor() {
        HonorModel honor = new HonorModel();
        honor.setId("honor-a");
        honor.setUserId("student-a");
        honor.setEvidenceUrl("/uploads/proof.pdf");
        honor.setCreateTime(java.time.LocalDateTime.now());
        return honor;
    }
}
