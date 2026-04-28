package org.example.repository;

import org.example.model.HonorModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface HonorRepository extends MongoRepository<HonorModel, String> {
    List<HonorModel> findByUserId(String userId);
    List<HonorModel> findByAuditStatus(String auditStatus);
}

