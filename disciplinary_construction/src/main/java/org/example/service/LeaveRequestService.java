package org.example.service;

import org.example.model.LeaveRequestModel;
import java.util.List;

public interface LeaveRequestService {
    // 学生提交请假申请
    LeaveRequestModel createLeaveRequest(LeaveRequestModel leaveRequest);
    
    // 根据ID查找请假记录
    LeaveRequestModel findById(String id);
    
    // 根据学生ID查找请假记录
    List<LeaveRequestModel> findByStudentId(String studentId);
    
    // 获取所有待审核的请假记录
    List<LeaveRequestModel> getPendingLeaveRequests();
    
    // 管理员审核请假
    LeaveRequestModel auditLeaveRequest(String id, String auditStatus, String auditComment, String auditorId, String auditorName);
    
    // 学生销假
    LeaveRequestModel checkIn(String id, String checkInComment, Double latitude, Double longitude, String address);
    
    // 自动更新请假状态（定时任务调用）
    void updateLeaveStatusAutomatically();
    
    // 获取所有请假记录（管理员）
    List<LeaveRequestModel> getAllLeaveRequests();
    
    // 更新请假记录（管理员手动更新状态等）
    LeaveRequestModel updateLeaveRequest(LeaveRequestModel leaveRequest);
}

