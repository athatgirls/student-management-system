package org.example;

import org.example.model.GradeModel;
import org.example.model.StudentModel;
import org.example.repository.GradeRepository;
import org.example.repository.StudentRepository;
import org.example.service.Impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void getAllGradesMergesDefaultsStudentGradesAndManagedGrades() {
        when(mongoTemplate.findDistinct(any(Query.class), eq("grade"), eq(StudentModel.class), eq(String.class)))
                .thenReturn(Arrays.asList("2020", "2027级", ""));
        when(gradeRepository.findAll()).thenReturn(Arrays.asList(grade("2028"), grade("2025级")));

        List<String> grades = studentService.getAllGrades();

        assertEquals(Arrays.asList("2020级", "2024级", "2025级", "2026级", "2027级", "2028级"), grades);
    }

    @Test
    void batchUpdateMajorUpdatesOnlyExistingRequestedStudents() {
        StudentModel existing = new StudentModel();
        existing.setId("student-1");
        existing.setMajor("旧专业");
        when(studentRepository.findById("student-1")).thenReturn(Optional.of(existing));
        when(studentRepository.findById("missing")).thenReturn(Optional.empty());

        int updatedCount = studentService.batchUpdateMajorByIds(Arrays.asList("student-1", "missing"), " 人工智能 ");

        assertEquals(1, updatedCount);
        assertEquals("人工智能", existing.getMajor());
        verify(studentRepository).save(existing);
    }

    private GradeModel grade(String gradeName) {
        GradeModel grade = new GradeModel();
        grade.setGradeName(gradeName);
        return grade;
    }
}
