package org.example;

import org.example.model.DailyTaskModel;
import org.example.model.PartyApplicationModel;
import org.example.model.StudentModel;
import org.example.service.DailyTaskAudienceService;
import org.example.repository.PartyApplicationRepository;
import java.util.Optional;
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
        PartyApplicationRepository partyApplicationRepository = mock(PartyApplicationRepository.class);
        PartyApplicationModel application = new PartyApplicationModel();
        application.setCurrentStage("入党积极分子");
        application.setAuditStatus("通过");
        // 入党申请按学生关联键（账号主键）查询，见 org.example.dto.StudentRef
        when(partyApplicationRepository.findByStudentAccountId("account-10240001")).thenReturn(Optional.of(application));

        audienceService = new DailyTaskAudienceService();
        ReflectionTestUtils.setField(audienceService, "partyApplicationRepository", partyApplicationRepository);

        student = new StudentModel();
        student.setId("account-10240001");
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
        assertThrows(
                IllegalArgumentException.class,
                () -> audienceService.validateAndNormalize(task));
    }

    @Test
    void legacyPartyStageNamesNormalizeToPoliticalStatusNames() {
        DailyTaskModel task = new DailyTaskModel();
        task.setAllowedIdentities(Collections.singletonList("预备党员"));
        audienceService.validateAndNormalize(task);
        assertEquals(Collections.singletonList("中共预备党员"), task.getAllowedIdentities());
    }
}
