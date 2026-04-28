package org.example.repository;

import org.example.model.GradeModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends MongoRepository<GradeModel, String> {
    Optional<GradeModel> findByGradeName(String gradeName);

    List<GradeModel> findAllByOrderByGradeNameAsc();
}
