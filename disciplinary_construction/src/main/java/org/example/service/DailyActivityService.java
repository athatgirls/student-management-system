package org.example.service;

import org.example.model.DailyActivityModel;
import java.util.List;

public interface DailyActivityService {
    DailyActivityModel create(DailyActivityModel model);
    List<DailyActivityModel> findAll();
    List<DailyActivityModel> findByStudentId(String studentId);
    DailyActivityModel findById(String id);
    DailyActivityModel audit(String id, String status, String comment, String auditorId);
    void delete(String id);
}

