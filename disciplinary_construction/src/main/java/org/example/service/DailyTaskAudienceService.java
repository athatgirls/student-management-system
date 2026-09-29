package org.example.service;

import org.example.model.DailyTaskModel;
import org.example.model.PartyApplicationModel;
import org.example.model.StudentModel;
import org.example.util.StudentGradePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一判断学生是否属于任务的接收范围，避免列表、提交和统计使用不同规则。
 */
@Service
public class DailyTaskAudienceService {

    public static final List<String> SUPPORTED_IDENTITIES = Collections.unmodifiableList(Arrays.asList(
            "中共党员", "中共预备党员", "发展对象", "入党积极分子", "共青团员", "群众"));

    @Autowired
    private PartyApplicationService partyApplicationService;

    public boolean matches(DailyTaskModel task, StudentModel student) {
        return matches(task, student, resolvePartyStage(student));
    }

    public void validateAndNormalize(DailyTaskModel task) {
        if (task == null) {
            throw new IllegalArgumentException("任务内容不能为空");
        }
        if (!isEmpty(task.getAllowedGrades())) {
            task.setAllowedGrades(task.getAllowedGrades().stream()
                    .map(StudentGradePolicy::requireValid)
                    .distinct()
                    .collect(Collectors.toList()));
        }
        if (!isEmpty(task.getAllowedIdentities())) {
            List<String> identities = task.getAllowedIdentities().stream()
                    .map(this::normalizeIdentity)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (identities.stream().anyMatch(identity -> !SUPPORTED_IDENTITIES.contains(identity))) {
                throw new IllegalArgumentException("任务接收政治面貌包含不支持的选项");
            }
            task.setAllowedIdentities(identities);
        }
    }

    public boolean matches(DailyTaskModel task, StudentModel student, String partyStage) {
        if (task == null || student == null) {
            return false;
        }

        if (!isEmpty(task.getAllowedGrades())) {
            String studentGrade = StudentGradePolicy.normalize(student.getGrade());
            boolean gradeMatched = task.getAllowedGrades().stream()
                    .map(StudentGradePolicy::normalize)
                    .anyMatch(grade -> grade != null && grade.equals(studentGrade));
            if (!gradeMatched) {
                return false;
            }
        }

        if (!isEmpty(task.getAllowedIdentities())) {
            Set<String> studentIdentities = resolveIdentities(student, partyStage);
            boolean identityMatched = task.getAllowedIdentities().stream()
                    .map(this::normalizeIdentity)
                    .anyMatch(studentIdentities::contains);
            if (!identityMatched) {
                return false;
            }
        }

        // 兼容已经发布的旧任务。
        if (!isEmpty(task.getAllowedPoliticalStatuses())) {
            String politicalStatus = trim(student.getPoliticalStatus());
            if (politicalStatus == null || !task.getAllowedPoliticalStatuses().contains(politicalStatus)) {
                return false;
            }
        }
        if (!isEmpty(task.getAllowedPartyStages())) {
            if (partyStage == null || !task.getAllowedPartyStages().contains(partyStage)) {
                return false;
            }
        }

        return true;
    }

    public String resolvePartyStage(StudentModel student) {
        if (student == null || trim(student.getStudentId()) == null) {
            return null;
        }
        PartyApplicationModel application = partyApplicationService.getByStudentId(student.getStudentId());
        return application == null || !"通过".equals(application.getAuditStatus()) ? null : trim(application.getCurrentStage());
    }

    public Set<String> resolveIdentities(StudentModel student, String partyStage) {
        Set<String> identities = new HashSet<>();
        if (student == null) {
            return identities;
        }

        addIdentity(identities, student.getPoliticalStatus());
        addIdentity(identities, partyStage);
        return identities;
    }

    private void addIdentity(Set<String> identities, String value) {
        String normalized = normalizeIdentity(value);
        if (normalized != null) {
            identities.add(normalized);
        }
    }

    private String normalizeIdentity(String value) {
        String identity = trim(value);
        if (identity == null) {
            return null;
        }
        switch (identity) {
            case "提交申请书":
                return "入党申请人";
            case "积极分子":
                return "入党积极分子";
            case "预备党员":
                return "中共预备党员";
            case "正式党员":
            case "党员":
                return "中共党员";
            default:
                return identity;
        }
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    private String trim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
