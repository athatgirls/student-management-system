package org.example;

import org.example.controller.GradeController;
import org.example.model.StudentModel;
import org.example.model.StudyRecordModel;
import org.example.repository.StudentRepository;
import org.example.repository.StudyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeManagementTest {
    @Mock private StudyRecordRepository studyRecordRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private MongoTemplate mongoTemplate;
    @InjectMocks private GradeController controller;

    @Test
    void onlyAdminsMayListAndEditGrades() {
        assertThrows(ResponseStatusException.class, () -> controller.getManagedRecords(null, null, null, null,
                1, 20, Map.of("userType", "student")));
        assertThrows(ResponseStatusException.class, () -> controller.updateManagedRecord("record-1", Map.of(),
                Map.of("userType", "student")));
        verify(mongoTemplate, never()).count(any(Query.class), eq(StudyRecordModel.class));
        verify(studyRecordRepository, never()).findById(any(String.class));
    }

    @Test
    void gradeCourseAndStudentFiltersAreCombinedBeforePagination() {
        StudentModel student = new StudentModel();
        student.setId("db-1"); student.setStudentId("10240001");
        when(studentRepository.findByGradeIn(any())).thenReturn(Collections.singletonList(student));
        when(mongoTemplate.count(any(Query.class), eq(StudyRecordModel.class))).thenReturn(1L);
        StudyRecordModel record = new StudyRecordModel();
        when(mongoTemplate.find(any(Query.class), eq(StudyRecordModel.class)))
                .thenReturn(Collections.singletonList(record));

        ResponseEntity<Map<String, Object>> response = controller.getManagedRecords(" 10240001 ", " 2025-2026-1 ",
                "2024", " Web ", 2, 10, Map.of("userType", "admin"));
        assertEquals(200, response.getBody().get("code"));
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertEquals(1L, data.get("total"));
        assertEquals(Collections.singletonList(record), data.get("records"));
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(query.capture(), eq(StudyRecordModel.class));
        assertEquals(10, query.getValue().getSkip());
        assertEquals(10, query.getValue().getLimit());
        String criteria = query.getValue().getQueryObject().toJson();
        assertTrue(criteria.contains("10240001"));
        assertTrue(criteria.contains("db-1"));
        assertTrue(criteria.contains("2025-2026-1"));
        assertTrue(criteria.contains("Web"));
    }

    @Test
    void rejectsUnboundedOrInvalidPageSizes() {
        assertThrows(ResponseStatusException.class, () -> controller.getManagedRecords(null, null, null, null,
                1, 101, Map.of("userType", "admin")));
        assertThrows(ResponseStatusException.class, () -> controller.getManagedRecords(null, null, null, null,
                0, 20, Map.of("userType", "admin")));
    }

    @Test
    void savesValidatedEditWithoutChangingStudentOrSource() {
        StudyRecordModel record = new StudyRecordModel();
        record.setId("record-1"); record.setStudentId("10240001"); record.setSource("official");
        when(studyRecordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(studyRecordRepository.findByStudentIdAndSemesterAndCourse("10240001", "2026秋", "数学"))
                .thenReturn(Optional.empty());
        when(studyRecordRepository.save(record)).thenReturn(record);

        controller.updateManagedRecord("record-1", Map.of("semester", "2026秋", "course", "数学",
                "score", 59, "credit", 2.0, "courseType", "degree"), Map.of("userType", "admin"));
        assertEquals(59, record.getScore());
        assertEquals("failed", record.getStatus());
        assertEquals("official", record.getSource());
        assertEquals("10240001", record.getStudentId());
        verify(studyRecordRepository).save(record);
    }

    @Test
    void rejectsInvalidScoreAndDuplicateCourse() {
        StudyRecordModel record = new StudyRecordModel();
        record.setId("record-1"); record.setStudentId("10240001");
        when(studyRecordRepository.findById("record-1")).thenReturn(Optional.of(record));
        Map<String, Object> input = Map.of("semester", "2026秋", "course", "数学", "score", 90,
                "credit", 2.0, "courseType", "degree");
        assertThrows(ResponseStatusException.class, () -> controller.updateManagedRecord("record-1",
                Map.of("semester", "2026秋", "course", "数学", "score", 101, "credit", 2,
                        "courseType", "degree"), Map.of("userType", "admin")));
        StudyRecordModel duplicate = new StudyRecordModel(); duplicate.setId("record-2");
        when(studyRecordRepository.findByStudentIdAndSemesterAndCourse("10240001", "2026秋", "数学"))
                .thenReturn(Optional.of(duplicate));
        assertThrows(ResponseStatusException.class, () -> controller.updateManagedRecord("record-1", input,
                Map.of("userType", "admin")));
        verify(studyRecordRepository, never()).save(any(StudyRecordModel.class));
    }
}
