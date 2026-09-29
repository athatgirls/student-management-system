package org.example.service.Impl;

import org.example.model.MajorModel;
import org.example.model.StudentModel;
import org.example.repository.MajorRepository;
import org.example.repository.StudentRepository;
import org.example.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class MajorServiceImpl implements MajorService {

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public MajorModel createMajor(MajorModel major) {
        Optional<MajorModel> existing = majorRepository.findByMajorName(major.getMajorName());
        if (existing.isPresent()) {
            throw new RuntimeException("该专业已存在");
        }
        major.setCreateTime(new Date());
        major.setUpdateTime(new Date());
        return majorRepository.save(major);
    }

    @Override
    public MajorModel findByMajorName(String majorName) {
        return majorRepository.findByMajorName(majorName).orElse(null);
    }

    @Override
    public List<String> getAllMajorNames() {
        Set<String> majors = new TreeSet<>();
        mongoTemplate.findDistinct(new Query(), "major", StudentModel.class, String.class).stream()
                .map(this::normalize)
                .filter(value -> !value.isEmpty())
                .forEach(majors::add);
        majorRepository.findAllByOrderByMajorNameAsc().stream()
                .map(MajorModel::getMajorName)
                .map(this::normalize)
                .filter(value -> !value.isEmpty())
                .forEach(majors::add);
        return new ArrayList<>(majors);
    }

    @Override
    public void deleteMajorByName(String majorName) {
        String normalized = normalize(majorName);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("请选择专业");
        }

        long studentCount = studentRepository.countByMajor(normalized);
        if (studentCount > 0) {
            throw new IllegalArgumentException("该专业下仍有 " + studentCount + " 名学生，无法删除");
        }

        MajorModel existing = findByMajorName(normalized);
        if (existing == null || !deleteMajor(existing.getId())) {
            throw new IllegalArgumentException("该专业不存在或非手动添加，无法删除");
        }
    }

    @Override
    public boolean deleteMajor(String id) {
        if (!majorRepository.existsById(id)) {
            return false;
        }
        majorRepository.deleteById(id);
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
