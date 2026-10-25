package org.example.repository;

import org.example.model.DailyTaskSubmissionModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface DailyTaskSubmissionRepository extends MongoRepository<DailyTaskSubmissionModel, String> {
    List<DailyTaskSubmissionModel> findByTaskId(String taskId);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<DailyTaskSubmissionModel> findByStudentAccountId(String studentAccountId);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    Optional<DailyTaskSubmissionModel> findByTaskIdAndStudentAccountId(String taskId, String studentAccountId);
}

