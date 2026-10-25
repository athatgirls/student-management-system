package org.example.repository;

import org.example.model.AppointmentModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<AppointmentModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<AppointmentModel> findByStudentAccountId(String studentAccountId);
}
