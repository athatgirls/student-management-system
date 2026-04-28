package org.example.repository;

import org.example.model.AcademicEventModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AcademicEventRepository extends MongoRepository<AcademicEventModel, String> {
    List<AcademicEventModel> findByStudentId(String studentId);
    List<AcademicEventModel> findByAuditStatus(String auditStatus);
}

