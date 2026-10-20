package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;
import org.example.repository.StudentRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentOverviewService {
    private final MongoTemplate mongo;
    private final StudentRepository students;
    private final CurrentUserAccessService access;
    private final ObjectMapper mapper;
    public StudentOverviewService(MongoTemplate mongo, StudentRepository students, CurrentUserAccessService access, ObjectMapper mapper) {
        this.mongo = mongo; this.students = students; this.access = access; this.mapper = mapper;
    }

    public Map<String, Object> overview(String studentId, String keyword, String grade, String studentIdKeyword, String major, int page, int size, Map<String, Object> user) {
        List<StudentModel> selected;
        if (!access.isAdmin(user)) {
            selected = List.of(access.requireCurrentStudent(user));
        } else if (studentId != null && !studentId.isBlank()) {
            StudentModel student = students.findById(studentId).orElseGet(() -> students.findByStudentId(studentId));
            if (student == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
            selected = List.of(student);
        } else {
            Criteria criteria = new Criteria();
            List<Criteria> filters = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String literal = java.util.regex.Pattern.quote(keyword.trim());
                filters.add(new Criteria().orOperator(Criteria.where("name").regex(literal, "i"), Criteria.where("studentId").regex(literal, "i")));
            }
            if (studentIdKeyword != null && !studentIdKeyword.isBlank())
                filters.add(Criteria.where("studentId").regex(java.util.regex.Pattern.quote(studentIdKeyword.trim()), "i"));
            if (grade != null && !grade.isBlank()) filters.add(Criteria.where("grade").is(grade));
            if (major != null && !major.isBlank()) filters.add(Criteria.where("major").is(major));
            if (!filters.isEmpty()) criteria.andOperator(filters.toArray(new Criteria[0]));
            Query query = Query.query(criteria);
            long total = mongo.count(query, StudentModel.class);
            selected = mongo.find(query.with(org.springframework.data.domain.Sort.by("studentId"))
                    .skip((long) (Math.max(1, page) - 1) * Math.min(100, Math.max(1, size))).limit(Math.min(100, Math.max(1, size))), StudentModel.class);
            return Map.of("students", summaries(selected), "total", total);
        }
        return Map.of("students", summaries(selected), "total", selected.size());
    }

    private List<Map<String, Object>> summaries(List<StudentModel> selected) {
        return selected.stream().map(this::summary).collect(Collectors.toList());
    }
    private Map<String, Object> summary(StudentModel student) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", student.getId()); result.put("studentId", student.getStudentId()); result.put("name", student.getName());
        result.put("grade", student.getGrade()); result.put("major", student.getMajor()); result.put("className", student.getClassName());
        result.put("supervisor", student.getSupervisor());
        result.put("grades", records(StudyRecordModel.class, student, "studentDbId", "studentId"));
        result.put("papers", records(PaperModel.class, student, "studentId"));
        result.put("competitions", records(CompetitionModel.class, student, "studentId"));
        result.put("patents", records(PatentModel.class, student, "studentId"));
        result.put("projects", records(ProjectModel.class, student, "studentId"));
        result.put("academic", records(AcademicEventModel.class, student, "studentId"));
        result.put("daily", records(DailyActivityModel.class, student, "studentId"));
        result.put("volunteer", records(VolunteerServiceModel.class, student, "studentId"));
        result.put("honors", records(HonorModel.class, student, "userId"));
        result.put("appointments", records(AppointmentModel.class, student, "studentDbId"));
        addTaskActivities(result, student);
        return result;
    }
    private <T> List<Map<String, Object>> records(Class<T> type, StudentModel student, String... fields) {
        List<Criteria> alternatives = new ArrayList<>();
        List<String> identifiers = new ArrayList<>();
        if (student.getId() != null && !student.getId().isBlank()) identifiers.add(student.getId());
        if (student.getStudentId() != null && !student.getStudentId().isBlank()) identifiers.add(student.getStudentId());
        for (String field : fields) {
            if (field.equals("studentDbId")) alternatives.add(Criteria.where(field).is(student.getId()));
            else if (type == StudyRecordModel.class) alternatives.add(new Criteria().andOperator(
                    new Criteria().orOperator(Criteria.where("studentDbId").is(null), Criteria.where("studentDbId").is("")),
                    Criteria.where(field).in(identifiers)));
            else alternatives.add(Criteria.where(field).in(identifiers));
        }
        return mongo.find(Query.query(new Criteria().orOperator(alternatives.toArray(new Criteria[0]))), type).stream()
                .map(record -> {
                    Map<String, Object> entry = mapper.convertValue(record, new TypeReference<Map<String, Object>>() { });
                    entry.put("studentId", student.getStudentId()); entry.put("studentName", student.getName());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private void addTaskActivities(Map<String, Object> result, StudentModel student) {
        List<DailyTaskSubmissionModel> submissions = mongo.find(Query.query(Criteria.where("studentId").is(student.getId())), DailyTaskSubmissionModel.class);
        if (submissions.isEmpty()) return;
        Map<String, DailyTaskModel> tasks = mongo.find(Query.query(Criteria.where("_id").in(submissions.stream()
                .map(DailyTaskSubmissionModel::getTaskId).collect(Collectors.toSet()))), DailyTaskModel.class).stream()
                .collect(Collectors.toMap(DailyTaskModel::getId, task -> task));
        Set<String> registrationTaskIds = tasks.values().stream()
                .filter(task -> "registration".equals(task.getTaskCategory()))
                .map(DailyTaskModel::getId).collect(Collectors.toSet());
        Set<String> attendedTaskIds = registrationTaskIds.isEmpty() ? Collections.emptySet() :
                mongo.find(Query.query(Criteria.where("taskId").in(registrationTaskIds)
                        .and("participantStudentIds").is(student.getId())), ActivityModel.class).stream()
                        .map(ActivityModel::getTaskId).collect(Collectors.toSet());
        for (DailyTaskSubmissionModel submission : submissions) {
            DailyTaskModel task = tasks.get(submission.getTaskId());
            if (task == null || "volunteer".equals(task.getActivityCategory())) continue;
            String category = "academic".equals(task.getActivityCategory()) ? "academic" : "daily";
            List<Map<String, Object>> activities = (List<Map<String, Object>>) result.get(category);
            // Only explicit task linkage is a duplicate; identical titles can describe distinct activities.
            if (activities.stream().anyMatch(row -> task.getId().equals(row.get("sourceTaskId")))) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", "submission:" + submission.getId()); entry.put("title", task.getTitle());
            entry.put("description", task.getDescription()); entry.put("source", "task");
            entry.put("studentId", student.getStudentId()); entry.put("studentName", student.getName());
            entry.put("taskCategory", task.getTaskCategory());
            entry.put("status", "registration".equals(task.getTaskCategory())
                    ? (attendedTaskIds.contains(task.getId()) ? "已完成" : "已报名") : submission.getStatus());
            entry.put("submissionTime", org.example.util.UtcTimestamps.toWire(submission.getSubmissionTime()));
            entry.put("deadline", task.getDeadline() == null ? null : task.getDeadline().toString());
            entry.put("content", submission.getContent());
            // Form content includes user-defined fields; preserve labels and make uploaded proofs downloadable.
            try {
                com.fasterxml.jackson.databind.JsonNode content = mapper.readTree(submission.getContent());
                if (content != null && content.isObject()) {
                    Map<String, Object> fields = mapper.convertValue(content, new TypeReference<Map<String, Object>>() { });
                    entry.put("submissionFields", fields);
                }
            } catch (java.io.IOException | IllegalArgumentException ignored) { /* Legacy free text remains in content. */ }
            activities.add(entry);
        }
    }
}
