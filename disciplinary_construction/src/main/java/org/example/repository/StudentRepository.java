package org.example.repository;

import org.example.model.StudentModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StudentRepository extends MongoRepository<StudentModel, String> {
    List<StudentModel> findByName(String name);

    StudentModel findByStudentId(String studentId);

    StudentModel findByEmail(String email);

    StudentModel findByPhone(String phone);

    StudentModel findByIdCard(String idCard);

    List<StudentModel> findByMajor(String major);

    List<StudentModel> findByGrade(String grade);

    List<StudentModel> findByStatus(String status);

    List<StudentModel> findByPoliticalStatus(String politicalStatus);
}
