package org.example.service;

import org.example.model.HonorModel;
import java.util.List;

public interface HonorService {
    HonorModel create(HonorModel model);
    List<HonorModel> findAll();
    List<HonorModel> findByUserId(String userId);
    HonorModel findById(String id);
    HonorModel update(HonorModel model);
    HonorModel audit(String id, String status, String comment, String auditorId);
    void delete(String id);
}
