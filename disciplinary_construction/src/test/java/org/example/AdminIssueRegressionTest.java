package org.example;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.controller.GradeController;
import org.example.model.ActivityModel;
import org.example.model.DailyTaskModel;
import org.example.repository.DailyTaskRepository;
import org.example.repository.ActivityRepository;
import org.example.service.Impl.ActivityServiceImpl;
import org.example.util.TaskSubmissionValidation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminIssueRegressionTest {
    @Test
    void templateEndpointReturnsReadableXlsxWithImportHeaders() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GradeController()).build();
        byte[] content = mvc.perform(get("/msi/grade/template"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Set<String> headers = new HashSet<>();
            workbook.getSheetAt(0).getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
            assertTrue(headers.containsAll(List.of("学号", "学期", "课程名称", "成绩")));
        }
    }

    @Test
    void mutationControllersOnlyExposeGetAndPostMappings() throws Exception {
        org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider scanner =
                new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(
                org.springframework.web.bind.annotation.RestController.class));
        for (org.springframework.beans.factory.config.BeanDefinition definition : scanner.findCandidateComponents("org.example.controller")) {
            for (java.lang.reflect.Method method : Class.forName(definition.getBeanClassName()).getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) continue;
                assertTrue(mapping.method().length > 0, method.toString());
                for (RequestMethod verb : mapping.method()) {
                    assertTrue(verb == RequestMethod.GET || verb == RequestMethod.POST, method + ": " + verb);
                }
            }
        }
    }

    @Test
    void publishingRejectsBlankTitleMissingTypeExpiredDeadlineAndDuplicateFields() {
        DailyTaskModel task = validTask();
        task.setTitle("  ");
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validateDefinition(task));
        task.setTitle("标题"); task.setType("");
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validateDefinition(task));
        task.setType("信息填写"); task.setDeadline(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validateDefinition(task));
        task.setDeadline(LocalDateTime.now().plusDays(2));
        DailyTaskModel.TaskField field = new DailyTaskModel.TaskField();
        field.setFieldName("姓名"); field.setFieldType("text");
        task.setFields(Arrays.asList(field, field));
        assertThrows(IllegalArgumentException.class, () -> TaskSubmissionValidation.validateDefinition(task));
        task.setFields(Collections.singletonList(field));
        assertDoesNotThrow(() -> TaskSubmissionValidation.validateDefinition(task));
    }

    @Test
    void activityDeadlineMustBeStrictlyAfterStartAndInvalidActivitiesAreNotSaved() {
        DailyTaskRepository tasks = mock(DailyTaskRepository.class);
        ActivityRepository activities = mock(ActivityRepository.class);
        ActivityServiceImpl service = new ActivityServiceImpl();
        ReflectionTestUtils.setField(service, "taskRepository", tasks);
        ReflectionTestUtils.setField(service, "activityRepository", activities);
        DailyTaskModel task = validTask();
        ActivityModel activity = new ActivityModel();
        activity.setTaskId("task-1"); activity.setActivityTime(task.getDeadline());
        when(tasks.findById("task-1")).thenReturn(Optional.of(task));
        assertThrows(IllegalArgumentException.class, () -> service.createActivity(activity));
        activity.setActivityTime(task.getDeadline().plusHours(1));
        assertThrows(IllegalArgumentException.class, () -> service.createActivity(activity));
        verifyNoInteractions(activities);
        activity.setActivityTime(task.getDeadline().minusHours(1));
        service.createActivity(activity);
        verify(activities).save(activity);
    }

    private DailyTaskModel validTask() {
        DailyTaskModel task = new DailyTaskModel();
        task.setTitle("日常任务"); task.setType("信息填写"); task.setDeadline(LocalDateTime.now().plusDays(2));
        return task;
    }
}
