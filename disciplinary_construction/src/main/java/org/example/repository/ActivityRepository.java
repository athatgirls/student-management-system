package org.example.repository;

import org.example.model.ActivityModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<ActivityModel, String> {
    List<ActivityModel> findByCreatorId(String creatorId);
    List<ActivityModel> findAllByOrderByActivityTimeDesc();
}

