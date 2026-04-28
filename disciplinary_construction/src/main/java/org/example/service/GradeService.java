package org.example.service;

import org.example.model.GradeModel;

import java.util.List;

public interface GradeService {
    GradeModel createGrade(GradeModel grade);

    GradeModel findById(String id);

    GradeModel findByGradeName(String gradeName);

    List<GradeModel> getAllGrades();

    boolean deleteGrade(String id);

    GradeModel updateGrade(GradeModel grade);
}
