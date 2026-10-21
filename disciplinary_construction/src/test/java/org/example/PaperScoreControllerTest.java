package org.example;

import org.example.controller.PaperController;
import org.example.model.PaperModel;
import org.example.repository.StudentRepository;
import org.example.service.CurrentUserAccessService;
import org.example.service.PaperService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaperScoreControllerTest {
    private final PaperService paperService = mock(PaperService.class);
    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final PaperController controller = new PaperController();

    private void setup() {
        ReflectionTestUtils.setField(controller, "paperService", paperService);
        ReflectionTestUtils.setField(controller, "currentUserAccessService",
                new CurrentUserAccessService(studentRepository));
        ReflectionTestUtils.setField(controller, "studentRepository", studentRepository);
    }

    @Test void onlyAdminCanMaintainStoredRatioAndScore() {
        setup();
        PaperModel paper = paper();
        when(paperService.getPaperById("paper-1")).thenReturn(paper);
        when(paperService.savePaper(any(PaperModel.class))).thenAnswer(call -> call.getArgument(0));
        PaperController.PaperScoreUpdate input = score("6:3:1", "0");

        assertThrows(AccessDeniedException.class, () -> controller.updatePaperScore("paper-1", input,
                Map.of("userType", "student")));
        verify(paperService, never()).savePaper(any());

        assertEquals(200, controller.updatePaperScore("paper-1", input, Map.of("userType", "admin")).getStatusCodeValue());
        assertEquals("6:3:1", paper.getScoreRatio());
        assertEquals(0, paper.getPaperScore().compareTo(BigDecimal.ZERO));
        assertEquals("待审核", paper.getAuditStatus());
        assertFalse(paper.isScorePending());
    }

    @Test void invalidRatioAndNegativeScoreAreRejectedWithoutSaving() {
        setup();
        when(paperService.getPaperById("paper-1")).thenReturn(paper());
        Map<String, Object> admin = Map.of("userType", "admin");
        assertEquals(400, controller.updatePaperScore("paper-1", score("4:6", "1"), admin).getStatusCodeValue());
        assertEquals(400, controller.updatePaperScore("paper-1", score("7:3", "-1"), admin).getStatusCodeValue());
        verify(paperService, never()).savePaper(any());
    }

    @Test void studentScoreEditsAppearImmediatelyAndRejectionRestoresOriginalValues() {
        setup();
        allowStudent();
        PaperModel existing = paper();
        existing.setScoreRatio("10:0");
        existing.setPaperScore(new BigDecimal("8.5"));
        when(paperService.getPaperById("paper-1")).thenReturn(existing);
        when(paperService.savePaper(any(PaperModel.class))).thenAnswer(call -> call.getArgument(0));
        PaperModel firstEdit = paper();
        firstEdit.setScoreRatio("7:3");
        firstEdit.setPaperScore(new BigDecimal("9"));

        assertEquals(200, controller.updatePaper("paper-1", firstEdit, true, studentUser()).getStatusCodeValue());
        assertEquals("7:3", firstEdit.getScoreRatio());
        assertTrue(firstEdit.isScorePending());
        assertEquals("10:0", firstEdit.getPreviousScoreRatio());
        assertEquals(0, firstEdit.getPreviousPaperScore().compareTo(new BigDecimal("8.5")));
        assertEquals("待审核", firstEdit.getAuditStatus());

        when(paperService.getPaperById("paper-1")).thenReturn(firstEdit);
        PaperModel secondEdit = paper();
        secondEdit.setScoreRatio("6:3:1");
        secondEdit.setPaperScore(new BigDecimal("12"));
        assertEquals(200, controller.updatePaper("paper-1", secondEdit, true, studentUser()).getStatusCodeValue());
        assertEquals("10:0", secondEdit.getPreviousScoreRatio());
        assertEquals(0, secondEdit.getPreviousPaperScore().compareTo(new BigDecimal("8.5")));
        when(paperService.getPaperById("paper-1")).thenReturn(secondEdit);

        assertEquals(200, controller.auditPaper("paper-1", Map.of("auditStatus", "已驳回", "auditComment", "重新核算"), adminUser()).getStatusCodeValue());
        assertEquals("10:0", secondEdit.getScoreRatio());
        assertEquals(0, secondEdit.getPaperScore().compareTo(new BigDecimal("8.5")));
        assertFalse(secondEdit.isScorePending());
        assertNull(secondEdit.getPreviousScoreRatio());
    }

    @Test void approvalKeepsNewScoresAndOldClientsCannotOverwriteThem() {
        setup();
        allowStudent();
        PaperModel existing = paper();
        existing.setScoreRatio("10:0");
        existing.setPaperScore(new BigDecimal("8.5"));
        when(paperService.getPaperById("paper-1")).thenReturn(existing);
        when(paperService.savePaper(any(PaperModel.class))).thenAnswer(call -> call.getArgument(0));

        PaperModel forged = paper();
        forged.setScoreRatio("7:3"); forged.setPaperScore(new BigDecimal("999"));
        assertEquals(200, controller.updatePaper("paper-1", forged, false, studentUser()).getStatusCodeValue());
        assertEquals("10:0", forged.getScoreRatio());
        assertEquals(0, forged.getPaperScore().compareTo(new BigDecimal("8.5")));

        PaperModel edit = paper();
        edit.setScoreRatio("7:3"); edit.setPaperScore(new BigDecimal("9"));
        assertEquals(200, controller.updatePaper("paper-1", edit, true, studentUser()).getStatusCodeValue());
        when(paperService.getPaperById("paper-1")).thenReturn(edit);
        assertEquals(200, controller.auditPaper("paper-1", Map.of("auditStatus", "已通过"), adminUser()).getStatusCodeValue());
        assertEquals("7:3", edit.getScoreRatio());
        assertEquals(0, edit.getPaperScore().compareTo(new BigDecimal("9")));
        assertFalse(edit.isScorePending());
        assertNull(edit.getPreviousPaperScore());
    }

    @Test void adminScoreMaintenanceSupersedesPendingStudentChange() {
        setup();
        PaperModel pending = paper();
        pending.setScorePending(true);
        pending.setPreviousScoreRatio("10:0");
        pending.setPreviousPaperScore(new BigDecimal("8"));
        when(paperService.getPaperById("paper-1")).thenReturn(pending);
        when(paperService.savePaper(any(PaperModel.class))).thenAnswer(call -> call.getArgument(0));
        controller.updatePaperScore("paper-1", score("6:3:1", "10"), adminUser());
        assertFalse(pending.isScorePending());
        assertNull(pending.getPreviousScoreRatio());
        controller.auditPaper("paper-1", Map.of("auditStatus", "已驳回"), adminUser());
        assertEquals("6:3:1", pending.getScoreRatio());
    }

    @Test void studentsCannotSetScoresOnCreation() {
        setup();
        allowStudent();

        PaperModel newPaper = paper();
        newPaper.setScoreRatio("7:3"); newPaper.setPaperScore(new BigDecimal("999"));
        when(paperService.addPaper(any(PaperModel.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals(200, controller.addPaper(newPaper,
                Map.of("userType", "student", "userId", "db-1", "username", "张三")).getStatusCodeValue());
        assertNull(newPaper.getScoreRatio());
        assertNull(newPaper.getPaperScore());
        assertEquals("张三", newPaper.getStudentName());
    }

    @Test void adminListFillsMissingNamesFromStudentsWithoutRewritingPapers() {
        setup();
        PaperModel missing = paper();
        PaperModel named = paper();
        named.setId("paper-2"); named.setStudentName("原有姓名");
        org.example.model.StudentModel student = new org.example.model.StudentModel();
        student.setStudentId("1001"); student.setName("张三");
        when(paperService.getAllPapers()).thenReturn(List.of(missing, named));
        when(studentRepository.findByStudentIdIn(List.of("1001"))).thenReturn(List.of(student));

        List<?> rows = (List<?>) controller.getAllPapers().getBody();
        assertNotNull(rows);
        assertEquals("张三", ((PaperModel) rows.get(0)).getStudentName());
        assertEquals("原有姓名", ((PaperModel) rows.get(1)).getStudentName());
        assertNull(missing.getStudentName());
        verify(paperService, never()).savePaper(any());
    }

    @Test void studentScoreChangesValidateRatioAndNonnegativeScore() {
        setup();
        allowStudent();
        when(paperService.getPaperById("paper-1")).thenReturn(paper());
        PaperModel invalidRatio = paper();
        invalidRatio.setScoreRatio("4:6");
        assertEquals(400, controller.updatePaper("paper-1", invalidRatio, true, studentUser()).getStatusCodeValue());
        PaperModel invalidScore = paper();
        invalidScore.setPaperScore(new BigDecimal("-1"));
        assertEquals(400, controller.updatePaper("paper-1", invalidScore, true, studentUser()).getStatusCodeValue());
        verify(paperService, never()).savePaper(any());
    }

    private void allowStudent() {
        org.example.model.StudentModel student = new org.example.model.StudentModel();
        student.setId("db-1"); student.setStudentId("1001"); student.setName("张三");
        when(studentRepository.findById("db-1")).thenReturn(Optional.of(student));
    }

    private Map<String, Object> studentUser() { return Map.of("userType", "student", "userId", "db-1"); }
    private Map<String, Object> adminUser() { return Map.of("userType", "admin", "userId", "admin-1", "username", "管理员"); }

    private PaperModel paper() {
        PaperModel paper = new PaperModel();
        paper.setId("paper-1");
        paper.setStudentId("1001");
        paper.setAttachments("[{\"url\":\"/proof.pdf\"}]");
        paper.setAuditStatus("待审核");
        return paper;
    }

    private PaperController.PaperScoreUpdate score(String ratio, String value) {
        PaperController.PaperScoreUpdate score = new PaperController.PaperScoreUpdate();
        score.setScoreRatio(ratio);
        score.setPaperScore(new BigDecimal(value));
        return score;
    }
}
