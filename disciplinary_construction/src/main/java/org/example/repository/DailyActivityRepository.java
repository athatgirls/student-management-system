package org.example.repository;

import org.example.model.DailyActivityModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DailyActivityRepository extends MongoRepository<DailyActivityModel, String> {
    List<DailyActivityModel> findByStudentId(String studentId);
    List<DailyActivityModel> findByAuditStatus(String auditStatus);
}

