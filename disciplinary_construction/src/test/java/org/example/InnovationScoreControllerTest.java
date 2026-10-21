package org.example;

import org.example.controller.CompetitionController;
import org.example.controller.PatentController;
import org.example.controller.ProjectController;
import org.example.model.CompetitionModel;
import org.example.model.PatentModel;
import org.example.model.ProjectModel;
import org.example.repository.StudentRepository;
import org.example.service.CompetitionService;
import org.example.service.CurrentUserAccessService;
import org.example.service.PatentService;
import org.example.service.ProjectService;
import org.example.model.StudentModel;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InnovationScoreControllerTest {
    private static final Map<String, Object> ADMIN = Map.of("userType", "admin", "userId", "admin-1");
    private static final Map<String, Object> STUDENT = Map.of("userType", "student", "userId", "db-1");

    private CurrentUserAccessService access() {
        StudentRepository repository = mock(StudentRepository.class);
        StudentModel student = new StudentModel();
        student.setId("db-1"); student.setStudentId("1001");
        when(repository.findById("db-1")).thenReturn(Optional.of(student));
        return new CurrentUserAccessService(repository);
    }

    @Test void competitionScoreIsProtectedAndRejectedEditsRestorePreviousValue() {
        CompetitionService service = mock(CompetitionService.class);
        CompetitionController controller = new CompetitionController();
        ReflectionTestUtils.setField(controller, "competitionService", service);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access());
        CompetitionModel stored = new CompetitionModel();
        stored.setId("1"); stored.setStudentId("1001"); stored.setScore(new BigDecimal("8"));
        when(service.getCompetitionById("1")).thenReturn(stored);
        when(service.saveCompetition(any())).thenAnswer(call -> call.getArgument(0));
        when(service.updateCompetition(any())).thenAnswer(call -> call.getArgument(0));
        CompetitionController.ScoreUpdate score = new CompetitionController.ScoreUpdate();
        score.setScore(new BigDecimal("9"));
        assertThrows(AccessDeniedException.class, () -> controller.updateCompetitionScore("1", score, STUDENT));
        score.setScore(new BigDecimal("-1"));
        assertEquals(400, controller.updateCompetitionScore("1", score, ADMIN).getStatusCodeValue());
        verify(service, never()).saveCompetition(any());
        score.setScore(new BigDecimal("8"));
        assertEquals(200, controller.updateCompetitionScore("1", score, ADMIN).getStatusCodeValue());
        CompetitionModel edit = new CompetitionModel();
        edit.setAttachments("[{\"url\":\"/proof.pdf\"}]"); edit.setTeamMembers("团队"); edit.setInstructorName("老师");
        edit.setScore(new BigDecimal("10"));
        assertEquals(200, controller.updateCompetition("1", edit, true, STUDENT).getStatusCodeValue());
        assertTrue(edit.isScorePending());
        assertEquals(0, edit.getPreviousScore().compareTo(new BigDecimal("8")));
        when(service.getCompetitionById("1")).thenReturn(edit);
        assertEquals(200, controller.auditCompetition("1", Map.of("auditStatus", "已驳回"), ADMIN).getStatusCodeValue());
        assertEquals(0, edit.getScore().compareTo(new BigDecimal("8")));
        assertFalse(edit.isScorePending());
    }

    @Test void patentScoreIsProtectedAndApprovalKeepsStudentEdit() {
        PatentService service = mock(PatentService.class);
        PatentController controller = new PatentController();
        ReflectionTestUtils.setField(controller, "patentService", service);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access());
        PatentModel stored = new PatentModel();
        stored.setId("1"); stored.setStudentId("1001"); stored.setScore(new BigDecimal("8"));
        when(service.getPatentById("1")).thenReturn(stored);
        when(service.savePatent(any())).thenAnswer(call -> call.getArgument(0));
        PatentController.ScoreUpdate score = new PatentController.ScoreUpdate();
        score.setScore(new BigDecimal("-1"));
        assertThrows(AccessDeniedException.class, () -> controller.updatePatentScore("1", score, STUDENT));
        assertEquals(400, controller.updatePatentScore("1", score, ADMIN).getStatusCodeValue());
        PatentModel edit = new PatentModel();
        edit.setAttachments("[{\"url\":\"/proof.pdf\"}]"); edit.setScore(new BigDecimal("10"));
        assertEquals(200, controller.updatePatent("1", edit, true, STUDENT).getStatusCodeValue());
        assertTrue(edit.isScorePending());
        when(service.getPatentById("1")).thenReturn(edit);
        assertEquals(200, controller.auditPatent("1", Map.of("auditStatus", "已通过"), ADMIN).getStatusCodeValue());
        assertEquals(0, edit.getScore().compareTo(new BigDecimal("10")));
        assertFalse(edit.isScorePending());
    }

    @Test void projectScoreCannotBeOverwrittenByOldClientAndAdminSupersedesPendingEdit() {
        ProjectService service = mock(ProjectService.class);
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "projectService", service);
        ReflectionTestUtils.setField(controller, "currentUserAccessService", access());
        ProjectModel stored = new ProjectModel();
        stored.setId("1"); stored.setStudentId("1001"); stored.setScore(new BigDecimal("8"));
        when(service.getProjectById("1")).thenReturn(stored);
        when(service.saveProject(any())).thenAnswer(call -> call.getArgument(0));
        ProjectModel edit = new ProjectModel();
        edit.setAttachments("[{\"url\":\"/proof.pdf\"}]"); edit.setTeamMembers("团队"); edit.setInstructorName("老师");
        edit.setScore(new BigDecimal("999"));
        assertEquals(200, controller.updateProject("1", edit, false, STUDENT).getStatusCodeValue());
        assertEquals(0, edit.getScore().compareTo(new BigDecimal("8")));
        edit.setScore(new BigDecimal("10"));
        assertEquals(200, controller.updateProject("1", edit, true, STUDENT).getStatusCodeValue());
        assertTrue(edit.isScorePending());
        when(service.getProjectById("1")).thenReturn(edit);
        ProjectController.ScoreUpdate score = new ProjectController.ScoreUpdate();
        score.setScore(new BigDecimal("12"));
        assertThrows(AccessDeniedException.class, () -> controller.updateProjectScore("1", score, STUDENT));
        score.setScore(new BigDecimal("-1"));
        assertEquals(400, controller.updateProjectScore("1", score, ADMIN).getStatusCodeValue());
        score.setScore(new BigDecimal("12"));
        assertEquals(200, controller.updateProjectScore("1", score, ADMIN).getStatusCodeValue());
        assertFalse(edit.isScorePending());
        controller.auditProject("1", Map.of("auditStatus", "已驳回"), ADMIN);
        assertEquals(0, edit.getScore().compareTo(new BigDecimal("12")));
    }
}
