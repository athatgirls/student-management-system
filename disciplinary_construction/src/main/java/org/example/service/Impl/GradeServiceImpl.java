package org.example.service.Impl;

import org.example.model.GradeModel;
import org.example.repository.GradeRepository;
import org.example.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private GradeRepository gradeRepository;

    @Override
    public GradeModel createGrade(GradeModel grade) {
        Optional<GradeModel> existing = gradeRepository.findByGradeName(grade.getGradeName());
        if (existing.isPresent()) {
            throw new RuntimeException("Grade name already exists");
        }

        grade.setCreateTime(new Date());
        grade.setUpdateTime(new Date());
        return gradeRepository.save(grade);
    }

    @Override
    public GradeModel findById(String id) {
        return gradeRepository.findById(id).orElse(null);
    }

    @Override
    public GradeModel findByGradeName(String gradeName) {
        return gradeRepository.findByGradeName(gradeName).orElse(null);
    }

    @Override
    public List<GradeModel> getAllGrades() {
        return gradeRepository.findAllByOrderByGradeNameAsc();
    }

    @Override
    public boolean deleteGrade(String id) {
        if (!gradeRepository.existsById(id)) {
            return false;
        }
        gradeRepository.deleteById(id);
        return true;
    }

    @Override
    public GradeModel updateGrade(GradeModel grade) {
        grade.setUpdateTime(new Date());
        return gradeRepository.save(grade);
    }
}
