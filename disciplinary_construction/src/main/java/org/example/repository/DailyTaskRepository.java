package org.example.repository;

import org.example.model.DailyTaskModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

import java.util.Optional;

public interface DailyTaskRepository extends MongoRepository<DailyTaskModel, String> {
    List<DailyTaskModel> findByActive(boolean active);
    List<DailyTaskModel> findByActiveAndAttachmentsContaining(boolean active, String attachment);
    Optional<DailyTaskModel> findByTitle(String title);
}

