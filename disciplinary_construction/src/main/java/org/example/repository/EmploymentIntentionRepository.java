package org.example.repository;
import org.example.model.EmploymentIntentionModel;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface EmploymentIntentionRepository extends MongoRepository<EmploymentIntentionModel, String> { }
