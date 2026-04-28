package org.example.repository;

import org.example.model.PartyApplicationModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyApplicationRepository extends MongoRepository<PartyApplicationModel, String> {
    Optional<PartyApplicationModel> findByStudentId(String studentId);
    List<PartyApplicationModel> findByCurrentStage(String currentStage);
    List<PartyApplicationModel> findByAuditStatus(String auditStatus);
    List<PartyApplicationModel> findByBranch(String branch);
    List<PartyApplicationModel> findByStudentIdContaining(String studentId);
    List<PartyApplicationModel> findByNameContaining(String name);
}

