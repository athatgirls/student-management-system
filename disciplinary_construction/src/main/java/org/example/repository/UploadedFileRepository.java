package org.example.repository;
import org.example.model.UploadedFileModel;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface UploadedFileRepository extends MongoRepository<UploadedFileModel, String> { }
