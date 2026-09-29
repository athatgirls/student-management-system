package org.example;

import org.example.model.MajorModel;
import org.example.model.StudentModel;
import org.example.repository.MajorRepository;
import org.example.repository.StudentRepository;
import org.example.service.Impl.MajorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MajorServiceImplTest {

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MajorServiceImpl majorService;

    @Test
    void getAllMajorNamesMergesStudentAndManagedMajorsInSortedOrder() {
        when(mongoTemplate.findDistinct(any(Query.class), eq("major"), eq(StudentModel.class), eq(String.class)))
                .thenReturn(Arrays.asList("软件工程", " 人工智能 ", ""));
        when(majorRepository.findAllByOrderByMajorNameAsc())
                .thenReturn(Arrays.asList(major("人工智能"), major("电子信息")));

        assertEquals(Arrays.asList("人工智能", "电子信息", "软件工程"), majorService.getAllMajorNames());
    }

    @Test
    void deleteMajorByNameRejectsMajorWithStudentsAndDoesNotDelete() {
        when(studentRepository.countByMajor("电子信息")).thenReturn(3L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> majorService.deleteMajorByName("电子信息"));

        assertEquals("该专业下仍有 3 名学生，无法删除", exception.getMessage());
        verify(majorRepository, never()).deleteById(any());
    }

    @Test
    void deleteMajorByNameRejectsEmptyAndBlankMajorWithoutRepositoryAccess() {
        IllegalArgumentException emptyException = assertThrows(IllegalArgumentException.class,
                () -> majorService.deleteMajorByName(null));
        IllegalArgumentException blankException = assertThrows(IllegalArgumentException.class,
                () -> majorService.deleteMajorByName("   "));

        assertEquals("请选择专业", emptyException.getMessage());
        assertEquals("请选择专业", blankException.getMessage());
        verifyNoInteractions(majorRepository, studentRepository);
    }

    @Test
    void deleteMajorByNameRejectsUnmanagedMajor() {
        when(studentRepository.countByMajor("人工智能")).thenReturn(0L);
        when(majorRepository.findByMajorName("人工智能")).thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> majorService.deleteMajorByName("人工智能"));

        assertEquals("该专业不存在或非手动添加，无法删除", exception.getMessage());
        verify(majorRepository, never()).deleteById(any());
    }

    @Test
    void deleteMajorByNameTrimsAndDeletesUnusedManagedMajor() {
        MajorModel major = major("电子信息");
        major.setId("major-1");
        when(studentRepository.countByMajor("电子信息")).thenReturn(0L);
        when(majorRepository.findByMajorName("电子信息")).thenReturn(java.util.Optional.of(major));
        when(majorRepository.existsById("major-1")).thenReturn(true);

        majorService.deleteMajorByName(" 电子信息 ");

        verify(studentRepository).countByMajor("电子信息");
        verify(majorRepository).deleteById("major-1");
    }

    private MajorModel major(String majorName) {
        MajorModel major = new MajorModel();
        major.setMajorName(majorName);
        return major;
    }
}
