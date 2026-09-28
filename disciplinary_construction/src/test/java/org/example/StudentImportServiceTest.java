package org.example;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.service.Impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentImportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void importsValidRowsAndReportsActualCount() throws Exception {
        when(studentRepository.findByStudentId("20240001")).thenReturn(null);
        when(studentRepository.save(any(StudentModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> summary = studentService.importStudents(
                new ByteArrayInputStream(workbook(Arrays.asList(
                        row("学号", "姓名", "专业", "年级", "班级"),
                        row("20240001", "张三", "软件工程", "2024级", "软工2401")))),
                "2024级");

        assertEquals(1, count(summary, "total"));
        assertEquals(1, count(summary, "imported"));
        assertEquals(0, count(summary, "invalid"));
        assertEquals(0, count(summary, "duplicate"));

        ArgumentCaptor<StudentModel> saved = ArgumentCaptor.forClass(StudentModel.class);
        verify(studentRepository).save(saved.capture());
        assertEquals("20240001", saved.getValue().getStudentId());
        assertEquals("2024级", saved.getValue().getGrade());
        assertTrue(saved.getValue().getPassword().startsWith("$2"));
    }

    @Test
    void reportsInvalidAndDuplicateRowsWithoutClaimingSuccess() throws Exception {
        StudentModel existing = new StudentModel();
        existing.setStudentId("20240002");
        when(studentRepository.findByStudentId("20240002")).thenReturn(existing);

        Map<String, Object> summary = studentService.importStudents(
                new ByteArrayInputStream(workbook(Arrays.asList(
                        row("学号", "姓名", "专业", "年级", "班级"),
                        row("", "缺少学号", "软件工程", "2024级", "软工2401"),
                        row("20240002", "李四", "软件工程", "2024级", "软工2401")))),
                "2024级");

        assertEquals(2, count(summary, "total"));
        assertEquals(0, count(summary, "imported"));
        assertEquals(1, count(summary, "invalid"));
        assertEquals(1, count(summary, "duplicate"));
        assertTrue(sampleErrors(summary).get(0).contains("第2行：学号为空"));
        verify(studentRepository, never()).save(any(StudentModel.class));
    }

    @Test
    void importsTitleRowAliasHeadersAndNumericStudentIds() throws Exception {
        when(studentRepository.save(any(StudentModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> summary = studentService.importStudents(
                new ByteArrayInputStream(workbook(Arrays.asList(
                        row("学生名单导入模板"),
                        row("学生学号 ", " 学生姓名", "专业名称"),
                        row("", "", ""),
                        row(20240003, "王五", "软件工程")))),
                "2024级");

        assertEquals(1, count(summary, "total"));
        assertEquals(1, count(summary, "imported"));
        assertEquals(0, count(summary, "invalid"));
        assertEquals(0, count(summary, "duplicate"));

        ArgumentCaptor<StudentModel> saved = ArgumentCaptor.forClass(StudentModel.class);
        verify(studentRepository).save(saved.capture());
        assertEquals("20240003", saved.getValue().getStudentId());
        assertEquals("软件工程", saved.getValue().getMajor());
    }

    @Test
    void reportsMissingRequiredHeader() throws Exception {
        Map<String, Object> summary = studentService.importStudents(
                new ByteArrayInputStream(workbook(Arrays.asList(
                        row("学号", "班级"),
                        row("20240004", "软工2401")))),
                "2024级");

        assertEquals(0, count(summary, "total"));
        assertEquals(0, count(summary, "imported"));
        assertTrue(sampleErrors(summary).get(0).contains("表头缺少“学号”或“姓名”列"));
        verify(studentRepository, never()).save(any(StudentModel.class));
    }

    private int count(Map<String, Object> summary, String key) {
        return ((Number) summary.get(key)).intValue();
    }

    @SuppressWarnings("unchecked")
    private List<String> sampleErrors(Map<String, Object> summary) {
        return (List<String>) summary.get("sampleErrors");
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private byte[] workbook(List<List<Object>> rows) {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writer.write(rows);
            writer.flush(outputStream, true);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            writer.close();
        }
    }
}
