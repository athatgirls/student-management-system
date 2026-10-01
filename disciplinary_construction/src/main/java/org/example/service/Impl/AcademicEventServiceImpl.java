package org.example.service.Impl;

import org.example.model.AcademicEventModel;
import org.example.repository.AcademicEventRepository;
import org.example.service.AcademicEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AcademicEventServiceImpl implements AcademicEventService {

    @Autowired
    private AcademicEventRepository repository;

    @Override
    public AcademicEventModel create(AcademicEventModel model) {
        org.example.util.SubmissionValidation.dates(model.getStartDate(), model.getEndDate());
        model.setId(null);
        model.setAuditStatus("pending");
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        return repository.save(model);
    }

    @Override
    public List<AcademicEventModel> findAll() {
        return repository.findAll();
    }

    @Override
    public List<AcademicEventModel> findByStudentId(String studentId) {
        return repository.findByStudentId(studentId);
    }

    @Override
    public AcademicEventModel findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public AcademicEventModel audit(String id, String status, String comment, String auditorId) {
        AcademicEventModel model = findById(id);
        if (model != null) {
            model.setAuditStatus(status);
            model.setAuditComment(comment);
            model.setAuditorId(auditorId);
            model.setAuditTime(LocalDateTime.now());
            model.setUpdateTime(LocalDateTime.now());
            return repository.save(model);
        }
        return null;
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
}

