package org.example.repository;

import org.example.model.LeaveRequestModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends MongoRepository<LeaveRequestModel, String> {
    // 根据学生ID查找请假记录
    List<LeaveRequestModel> findByStudentId(String studentId);
    
    // 根据审核状态查找
    List<LeaveRequestModel> findByAuditStatus(String auditStatus);
    
    // 根据请假状态查找
    List<LeaveRequestModel> findByStatus(String status);
    
    // 查找需要自动更新状态的请假记录（已批准且开始时间已到）
    List<LeaveRequestModel> findByAuditStatusAndStatusAndStartDateLessThanEqual(String auditStatus, String status, LocalDate date);
    
    // 查找需要提醒销假的请假记录（已结束但未销假）
    List<LeaveRequestModel> findByStatusAndEndDateLessThanEqual(String status, LocalDate date);
}

