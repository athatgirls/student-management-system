package org.example.repository;

import org.example.model.MajorModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends MongoRepository<MajorModel, String> {
    Optional<MajorModel> findByMajorName(String majorName);

    List<MajorModel> findAllByOrderByMajorNameAsc();
}
