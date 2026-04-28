package org.example.repository;

import org.example.model.DailyTaskSubmissionModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface DailyTaskSubmissionRepository extends MongoRepository<DailyTaskSubmissionModel, String> {
    List<DailyTaskSubmissionModel> findByTaskId(String taskId);
    List<DailyTaskSubmissionModel> findByStudentId(String studentId);
    Optional<DailyTaskSubmissionModel> findByTaskIdAndStudentId(String taskId, String studentId);
}

