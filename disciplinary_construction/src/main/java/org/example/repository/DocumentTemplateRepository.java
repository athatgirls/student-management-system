package org.example.repository;

import org.example.model.DocumentTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentTemplateRepository extends MongoRepository<DocumentTemplate, String> { }
