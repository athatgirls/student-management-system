package org.example;

import org.example.model.MajorModel;
import org.example.model.StudentModel;
import org.example.repository.MajorRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MajorServiceImplTest {

    @Mock
    private MajorRepository majorRepository;

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

    private MajorModel major(String majorName) {
        MajorModel major = new MajorModel();
        major.setMajorName(majorName);
        return major;
    }
}
