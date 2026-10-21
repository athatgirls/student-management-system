package org.example;

import org.example.controller.StatisticsController;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {
    @Mock private CompetitionService competitionService;
    @Mock private ProjectService projectService;
    @Mock private PaperService paperService;
    @Mock private PatentService patentService;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private AcademicEventRepository academicEventRepository;
    @Mock private DailyActivityRepository dailyActivityRepository;
    @Mock private HonorRepository honorRepository;
    @Mock private VolunteerServiceService volunteerServiceService;
    @InjectMocks private StatisticsController controller;

    @Test
    void dailyPendingTotalIncludesEachCategoryAndTracksVolunteerChanges() {
        when(leaveRequestRepository.findByAuditStatus("pending"))
                .thenReturn(Collections.nCopies(2, new LeaveRequestModel()));
        when(academicEventRepository.findByAuditStatus("pending"))
                .thenReturn(Collections.nCopies(3, new AcademicEventModel()));
        when(dailyActivityRepository.findByAuditStatus("pending"))
                .thenReturn(Collections.nCopies(4, new DailyActivityModel()));
        when(honorRepository.findByAuditStatus("pending"))
                .thenReturn(Collections.nCopies(5, new HonorModel()));
        when(volunteerServiceService.getPendingVolunteerServices())
                .thenReturn(Collections.nCopies(6, new VolunteerServiceModel()), Collections.emptyList());

        Map<?, ?> daily = dailyStats();
        assertEquals(6, daily.get("volunteer"));
        int sum = java.util.List.of("leave", "academic", "dailyActivity", "honor", "volunteer")
                .stream().mapToInt(key -> (Integer) daily.get(key)).sum();
        assertEquals(20, sum);
        assertEquals(sum, daily.get("pending"));

        Map<?, ?> updated = dailyStats();
        assertEquals(0, updated.get("volunteer"));
        assertEquals(14, updated.get("pending"));
    }

    private Map<?, ?> dailyStats() {
        org.springframework.http.ResponseEntity<?> response = controller.getAuditStats();
        assertEquals(200, response.getStatusCodeValue());
        return (Map<?, ?>) ((Map<?, ?>) response.getBody()).get("daily");
    }
}
