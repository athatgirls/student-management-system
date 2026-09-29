package org.example;

import org.example.model.DailyTaskModel;
import org.example.model.PartyApplicationModel;
import org.example.model.StudentModel;
import org.example.service.DailyTaskAudienceService;
import org.example.service.PartyApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyTaskAudienceServiceTest {

    private DailyTaskAudienceService audienceService;
    private StudentModel student;

    @BeforeEach
    void setUp() {
        PartyApplicationService partyApplicationService = mock(PartyApplicationService.class);
        PartyApplicationModel application = new PartyApplicationModel();
        application.setCurrentStage("入党积极分子");
        application.setAuditStatus("通过");
        when(partyApplicationService.getByStudentId("10240001")).thenReturn(application);

        audienceService = new DailyTaskAudienceService();
        ReflectionTestUtils.setField(audienceService, "partyApplicationService", partyApplicationService);

        student = new StudentModel();
        student.setStudentId("10240001");
        student.setGrade("2024级");
        student.setPoliticalStatus("共青团员");
    }

    @Test
    void activistReceivesActivistTask() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedIdentities(Collections.singletonList("入党积极分子"));
        assertTrue(audienceService.matches(task, student));
    }

    @Test
    void unrelatedIdentityCannotSeeTask() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedIdentities(Collections.singletonList("中共党员"));
        assertFalse(audienceService.matches(task, student));
    }

    @Test
    void multipleSelectedIdentitiesUseOrSemantics() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedIdentities(Arrays.asList("发展对象", "共青团员"));
        assertTrue(audienceService.matches(task, student));
    }

    @Test
    void gradeMatchingAcceptsManualYearWithOrWithoutSuffix() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedGrades(Collections.singletonList("2024"));
        assertTrue(audienceService.matches(task, student));
    }

    @Test
    void unsupportedIdentityIsRejectedWhenPublishing() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedIdentities(Collections.singletonList("未知身份"));
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> audienceService.validateAndNormalize(task));
        assertEquals("任务接收政治面貌包含不支持的选项", error.getMessage());
    }

    @Test
    void selectableIdentitiesMatchStudentPoliticalStatusOptions() {
        assertEquals(Arrays.asList(
                "中共党员", "中共预备党员", "发展对象", "入党积极分子", "共青团员", "群众"),
                DailyTaskAudienceService.SUPPORTED_IDENTITIES);
    }

    @Test
    void legacyPartyStageNamesNormalizeToPoliticalStatusNames() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedIdentities(Collections.singletonList("预备党员"));
        audienceService.validateAndNormalize(task);
        assertEquals(Collections.singletonList("中共预备党员"), task.getAllowedIdentities());
    }
}
