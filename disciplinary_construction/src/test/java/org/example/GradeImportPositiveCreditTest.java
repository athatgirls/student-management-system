package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.GradeController;
import org.example.repository.StudentRepository;
import org.example.repository.StudyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GradeImportPositiveCreditTest {
    @InjectMocks private GradeController controller;
    @Mock private StudentRepository students;
    @Mock private StudyRecordRepository records;

    @Test
    void rejectsMissingZeroNegativeNonfiniteAndMalformedCreditBeforeSaving() {
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        for (String credit : new String[] { "null", "0", "-1", "\"NaN\"", "\"Infinity\"", "\"abc\"" }) {
            String json = "[{\"studentId\":\"10240001\",\"semester\":\"2026秋\","
                    + "\"courseName\":\"课程\",\"score\":90,\"credit\":" + credit + "}]";
            ResponseEntity<Map<String, Object>> response = controller.importGrades(json, null, Map.of("userType", "admin"));
            assertEquals(200, response.getBody().get("code"));
            Map<?, ?> summary = (Map<?, ?>) response.getBody().get("data");
            assertEquals(1, summary.get("skippedCount"));
            assertEquals(0, summary.get("createdCount"));
            assertEquals(0, summary.get("updatedCount"));
            assertTrue(((java.util.List<?>) summary.get("invalidRows")).get(0).toString().contains("credit"));
        }
        verifyNoInteractions(students, records);
    }
}
