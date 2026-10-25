package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学生标识命名防误用守护（约定见 {@link org.example.dto.StudentRef}）：
 * studentAccountId = 账号主键（业务关联键），studentId = 学号（仅展示/导入匹配）。
 * 用反射做硬约束，防止 studentDbId / userId / 裸 findByStudentId 等历史别名回潮。
 */
class StudentAssociationNamingTest {

    /** 含学生关联的业务模型，必须声明 studentAccountId。 */
    private static final List<String> BUSINESS_MODELS = List.of(
            "AcademicEventModel", "AlumniModel", "AppointmentModel",
            "CompetitionModel", "DailyActivityModel", "DailyTaskSubmissionModel",
            "EmploymentIntentionModel", "HonorModel", "IdeologicalTweetModel",
            "InternshipEmploymentModel", "LeaveRequestModel", "PaperModel",
            "PartyApplicationModel", "PartyCourseModel", "PartyMemberModel",
            "PatentModel", "ProjectModel", "StudyRecordModel", "ThoughtReportModel",
            "VolunteerServiceModel");

    /** 全部模型类（含账号/字典类），禁止出现的历史别名字段。 */
    private static final List<String> ALL_MODELS = List.of(
            "AcademicEventModel", "ActivityModel", "AdminModel", "AlumniModel", "AppointmentModel",
            "CompetitionDictModel", "CompetitionModel", "DailyActivityModel", "DailyTaskModel",
            "DailyTaskSubmissionModel", "DocumentTemplate", "EmploymentIntentionModel", "GradeModel",
            "HonorModel", "IdeologicalTweetModel", "InternshipEmploymentModel", "LeaveRequestModel",
            "MajorModel", "PaperModel", "PartyApplicationModel", "PartyCourseModel", "PartyMemberModel",
            "PatentModel", "ProjectModel", "StudentModel", "StudyRecordModel", "ThoughtReportModel",
            "UploadedFileModel", "VolunteerServiceModel");

    @Test
    void businessModelsDeclareStudentAccountId() throws Exception {
        for (String name : BUSINESS_MODELS) {
            Field field = findField(model(name), "studentAccountId");
            assertTrue(field != null, name + " 必须声明 studentAccountId（学生关联键，见 StudentRef 约定）");
        }
    }

    @Test
    void modelsForbidLegacyAliasFields() throws Exception {
        for (String name : ALL_MODELS) {
            for (String banned : List.of("studentDbId", "userId", "participantStudentIds")) {
                assertTrue(findField(model(name), banned) == null,
                        name + " 不允许再出现历史别名字段 " + banned + "（见 StudentRef 约定）");
            }
        }
    }

    @Test
    void repositoriesForbidAmbiguousStudentFinders() throws Exception {
        for (Class<?> repo : List.of(
                "AcademicEventRepository", "ActivityRepository", "AdminRepository", "AlumniRepository",
                "AppointmentRepository", "CompetitionDictRepository", "CompetitionRepository",
                "DailyActivityRepository", "DailyTaskRepository", "DailyTaskSubmissionRepository",
                "DocumentTemplateRepository", "EmploymentIntentionRepository", "GradeRepository",
                "HonorRepository", "IdeologicalTweetRepository", "InternshipEmploymentRepository",
                "LeaveRequestRepository", "MajorRepository", "PaperRepository", "PartyApplicationRepository",
                "PartyCourseRepository", "PartyMemberRepository", "PatentRepository", "ProjectRepository",
                "StudyRecordRepository", "ThoughtReportRepository", "UploadedFileRepository",
                "VolunteerServiceRepository").stream().map(n -> {
                    try {
                        return Class.forName("org.example.repository." + n);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("缺少仓储 " + n, e);
                    }
                }).toArray(Class<?>[]::new)) {
            for (Method method : repo.getDeclaredMethods()) {
                String n = method.getName();
                boolean ambiguous = (n.contains("StudentId") || n.contains("UserId"))
                        && !n.contains("StudentAccountId");
                assertFalse(ambiguous, repo.getSimpleName() + "." + n
                        + " 命名歧义：业务关联一律 StudentAccountId（见 StudentRef 约定）；学号查询只允许在 StudentRepository");
            }
        }
    }

    private Class<?> model(String simpleName) {
        try {
            return Class.forName("org.example.model." + simpleName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("缺少模型 " + simpleName, e);
        }
    }

    private Field findField(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
