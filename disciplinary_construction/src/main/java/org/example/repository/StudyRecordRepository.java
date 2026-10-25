package org.example.repository;

import org.example.model.StudyRecordModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudyRecordRepository extends MongoRepository<StudyRecordModel, String> {
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<StudyRecordModel> findByStudentAccountId(String studentAccountId);
    List<StudyRecordModel> findBySemester(String semester);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    List<StudyRecordModel> findByStudentAccountIdAndSemester(String studentAccountId, String semester);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。本接口不提供 findByStudentId 这类歧义命名。 */
    Optional<StudyRecordModel> findByStudentAccountIdAndSemesterAndCourse(String studentAccountId, String semester, String course);
}
