package org.example.repository;

import org.example.model.StudyRecordModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudyRecordRepository extends MongoRepository<StudyRecordModel, String> {
    List<StudyRecordModel> findByStudentDbId(String studentDbId);
    List<StudyRecordModel> findByStudentId(String studentId);
    List<StudyRecordModel> findBySemester(String semester);
    List<StudyRecordModel> findByStudentIdAndSemester(String studentId, String semester);
    Optional<StudyRecordModel> findByStudentIdAndSemesterAndCourse(String studentId, String semester, String course);
}
