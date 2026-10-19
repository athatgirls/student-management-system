package org.example.repository;

import org.example.model.AppointmentModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<AppointmentModel, String> {
    List<AppointmentModel> findByStudentDbId(String studentDbId);
}
