package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentOverviewServiceTest {
    @Test void adminFiltersByGradeAndPartialStudentNumberBeforePaginating() {
        StudentRepository students = mock(StudentRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.count(any(Query.class), eq(StudentModel.class))).thenReturn(37L);
        when(mongo.find(any(Query.class), eq(StudentModel.class))).thenReturn(Collections.emptyList());
        StudentOverviewService service = new StudentOverviewService(mongo, students,
                new CurrentUserAccessService(students), new ObjectMapper());

        Map<String, Object> result = service.overview(null, "张", "2025级", " 10.* ", null, 2, 20,
                Map.of("userType", "admin"));

        assertEquals(37L, result.get("total"));
        ArgumentCaptor<Query> counted = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> fetched = ArgumentCaptor.forClass(Query.class);
        verify(mongo).count(counted.capture(), eq(StudentModel.class));
        verify(mongo).find(fetched.capture(), eq(StudentModel.class));
        String filter = counted.getValue().getQueryObject().toJson();
        assertEquals(filter, fetched.getValue().getQueryObject().toJson());
        assertTrue(filter.contains("2025级"));
        assertTrue(filter.contains("name"));
        assertTrue(filter.contains("studentId"));
        assertTrue(filter.contains("\\\\Q10.*\\\\E"));
        assertEquals(20, fetched.getValue().getSkip());
        assertEquals(20, fetched.getValue().getLimit());
        verify(students, never()).findByStudentId(anyString());
    }

    @Test void studentQueryIgnoresRequestedOtherStudentAndFiltersAllCollections() {
        StudentRepository students = mock(StudentRepository.class);
        StudentModel own = new StudentModel(); own.setId("db-a"); own.setStudentId("1001");
        when(students.findById("db-a")).thenReturn(Optional.of(own));
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), any(Class.class))).thenAnswer(invocation -> {
            String query = ((Query) invocation.getArgument(0)).getQueryObject().toJson();
            assertTrue(query.contains("db-a")); assertFalse(query.contains("db-b"));
            Class<?> type = invocation.getArgument(1);
            if (type == org.example.model.AppointmentModel.class || type == org.example.model.DailyTaskSubmissionModel.class) assertFalse(query.contains("1001"));
            else assertTrue(query.contains("1001"));
            if (type == org.example.model.StudyRecordModel.class) {
                assertTrue(query.contains("$and")); assertTrue(query.contains("\"studentDbId\": null"));
            }
            return Collections.emptyList();
        });
        Map<String, Object> result = new StudentOverviewService(mongo, students, new CurrentUserAccessService(students), new ObjectMapper())
                .overview("db-b", null, null, null, null, 1, 10, Map.of("userId", "db-a", "userType", "student"));
        assertEquals(1, result.get("total"));
        verify(students, never()).findById("db-b"); verify(students, never()).findByStudentId("db-b");
        verify(mongo, times(11)).find(any(Query.class), any(Class.class));
    }

    @Test void taskSubmissionsAppearInTheirActivitySectionsWithFullFormFields() {
        StudentRepository students = mock(StudentRepository.class);
        StudentModel own = new StudentModel(); own.setId("db-a"); own.setStudentId("1001");
        when(students.findById("db-a")).thenReturn(Optional.of(own));
        MongoTemplate mongo = mock(MongoTemplate.class);
        org.example.model.DailyTaskSubmissionModel submission = new org.example.model.DailyTaskSubmissionModel();
        submission.setId("submission-a"); submission.setTaskId("task-a"); submission.setStudentId("db-a"); submission.setStatus("completed");
        submission.setContent("{\"学习心得\":\"讲座内容\",\"证明\":\"[{\\\"url\\\":\\\"/SCSE@hbut/uploads/proof.pdf\\\"}]\"}");
        org.example.model.DailyTaskModel task = new org.example.model.DailyTaskModel(); task.setId("task-a"); task.setTitle("学术讲座"); task.setActivityCategory("academic");
        when(mongo.find(any(Query.class), any(Class.class))).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(1);
            if (type == org.example.model.DailyTaskSubmissionModel.class) return List.of(submission);
            if (type == org.example.model.DailyTaskModel.class) return List.of(task);
            return Collections.emptyList();
        });
        Map<String, Object> result = new StudentOverviewService(mongo, students, new CurrentUserAccessService(students), new ObjectMapper())
                .overview(null, null, null, null, null, 1, 10, Map.of("userId", "db-a", "userType", "student"));
        Map<?, ?> summary = (Map<?, ?>) ((List<?>) result.get("students")).get(0);
        Map<?, ?> activity = (Map<?, ?>) ((List<?>) summary.get("academic")).get(0);
        assertEquals("学术讲座", activity.get("title"));
        assertEquals("讲座内容", ((Map<?, ?>) activity.get("submissionFields")).get("学习心得"));
        assertTrue(((List<?>) summary.get("daily")).isEmpty());
    }

    @Test void attendanceListDistinguishesRegisteredFromAttendedEvenForExistingSubmissions() {
        StudentRepository students = mock(StudentRepository.class);
        StudentModel own = new StudentModel(); own.setId("db-a"); own.setStudentId("1001");
        when(students.findById("db-a")).thenReturn(Optional.of(own));
        MongoTemplate mongo = mock(MongoTemplate.class);
        org.example.model.DailyTaskSubmissionModel submission = new org.example.model.DailyTaskSubmissionModel();
        submission.setId("submission-a"); submission.setTaskId("task-a"); submission.setStudentId("db-a");
        submission.setStatus("completed"); // Registration alone already uses this status.
        submission.setContent("{\"报名理由\":\"参加讲座\"}");
        org.example.model.DailyTaskModel task = new org.example.model.DailyTaskModel();
        task.setId("task-a"); task.setTitle("讲座"); task.setTaskCategory("registration"); task.setActivityCategory("academic");
        org.example.model.ActivityModel activity = new org.example.model.ActivityModel();
        activity.setId("activity-a"); activity.setTaskId("task-a"); activity.setParticipantStudentIds(List.of("db-a"));
        when(mongo.find(any(Query.class), any(Class.class))).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(1);
            if (type == org.example.model.DailyTaskSubmissionModel.class) return List.of(submission);
            if (type == org.example.model.DailyTaskModel.class) return List.of(task);
            if (type == org.example.model.ActivityModel.class) {
                String query = ((Query) invocation.getArgument(0)).getQueryObject().toJson();
                assertTrue(query.contains("participantStudentIds"));
                assertTrue(query.contains("db-a"));
                return activity.getParticipantStudentIds().contains("db-a") ? List.of(activity) : Collections.emptyList();
            }
            return Collections.emptyList();
        });
        StudentOverviewService service = new StudentOverviewService(mongo, students,
                new CurrentUserAccessService(students), new ObjectMapper());
        Map<String, Object> user = Map.of("userId", "db-a", "userType", "student");
        Map<?, ?> attended = (Map<?, ?>) ((List<?>) service.overview(null, null, null, null, null, 1, 10, user).get("students")).get(0);
        assertEquals("已完成", ((Map<?, ?>) ((List<?>) attended.get("academic")).get(0)).get("status"));

        activity.setParticipantStudentIds(Collections.emptyList());
        Map<?, ?> registered = (Map<?, ?>) ((List<?>) service.overview(null, null, null, null, null, 1, 10, user).get("students")).get(0);
        assertEquals("已报名", ((Map<?, ?>) ((List<?>) registered.get("academic")).get(0)).get("status"));
    }
}
