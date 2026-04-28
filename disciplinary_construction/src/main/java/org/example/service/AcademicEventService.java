package org.example.service;

import org.example.model.AcademicEventModel;
import java.util.List;

public interface AcademicEventService {
    AcademicEventModel create(AcademicEventModel model);
    List<AcademicEventModel> findAll();
    List<AcademicEventModel> findByStudentId(String studentId);
    AcademicEventModel findById(String id);
    AcademicEventModel audit(String id, String status, String comment, String auditorId);
    void delete(String id);
}

