package org.example.service.Impl;

import org.example.model.DailyActivityModel;
import org.example.repository.DailyActivityRepository;
import org.example.service.DailyActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DailyActivityServiceImpl implements DailyActivityService {

    @Autowired
    private DailyActivityRepository repository;

    @Override
    public DailyActivityModel create(DailyActivityModel model) {
        model.setAuditStatus("pending");
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        return repository.save(model);
    }

    @Override
    public List<DailyActivityModel> findAll() {
        return repository.findAll();
    }

    @Override
    public List<DailyActivityModel> findByStudentId(String studentId) {
        return repository.findByStudentId(studentId);
    }

    @Override
    public DailyActivityModel findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public DailyActivityModel audit(String id, String status, String comment, String auditorId) {
        DailyActivityModel model = findById(id);
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

