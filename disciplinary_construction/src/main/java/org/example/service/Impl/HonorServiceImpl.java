package org.example.service.Impl;

import org.example.model.HonorModel;
import org.example.repository.HonorRepository;
import org.example.service.HonorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HonorServiceImpl implements HonorService {

    @Autowired
    private HonorRepository repository;

    @Override
    public HonorModel create(HonorModel model) {
        model.setAuditStatus("pending");
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        return repository.save(model);
    }

    @Override
    public List<HonorModel> findAll() {
        return repository.findAll();
    }

    @Override
    public List<HonorModel> findByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public HonorModel findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public HonorModel audit(String id, String status, String comment, String auditorId) {
        HonorModel model = findById(id);
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

