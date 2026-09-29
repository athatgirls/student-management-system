package org.example.service.Impl;

import org.example.model.LeaveRequestModel;
import org.example.model.StudentModel;
import org.example.repository.LeaveRequestRepository;
import org.example.service.LeaveRequestService;
import org.example.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private StudentService studentService;
    
    @Override
    public LeaveRequestModel createLeaveRequest(LeaveRequestModel leaveRequest) {
        leaveRequest.setId(null);
        // 必须提供studentId（学号），自动从学生表获取所有信息
        if (leaveRequest.getStudentId() == null || leaveRequest.getStudentId().isEmpty()) {
            throw new RuntimeException("学号不能为空");
        }
        
        // 根据学号查找学生（注意：这里studentId是学号，不是MongoDB的_id）
        StudentModel student = studentService.findByStudentId(leaveRequest.getStudentId());
        if (student == null) {
            throw new RuntimeException("未找到该学号对应的学生信息");
        }
        
        // 检查必需信息是否完整
        StringBuilder missingFields = new StringBuilder();
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            missingFields.append("姓名、");
        }
        if (student.getGrade() == null || student.getGrade().trim().isEmpty()) {
            missingFields.append("年级、");
        }
        if (student.getMajor() == null || student.getMajor().trim().isEmpty()) {
            missingFields.append("专业、");
        }
        if (student.getClassName() == null || student.getClassName().trim().isEmpty()) {
            missingFields.append("班级、");
        }
        if (student.getPhone() == null || student.getPhone().trim().isEmpty()) {
            missingFields.append("电话、");
        }
        
        if (missingFields.length() > 0) {
            String fields = missingFields.substring(0, missingFields.length() - 1); // 移除最后的顿号
            throw new RuntimeException("个人信息不完整，请先完善个人信息后再提交请假申请。缺少信息：" + fields);
        }
        
        // 自动填充所有学生信息
        leaveRequest.setStudentName(student.getName());
        leaveRequest.setGrade(student.getGrade());
        leaveRequest.setMajor(student.getMajor());
        leaveRequest.setClassName(student.getClassName());
        leaveRequest.setPhone(student.getPhone());
        
        // 计算请假天数
        if (leaveRequest.getStartDate() != null && leaveRequest.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
            leaveRequest.setDays((int) days);
        }
        
        leaveRequest.setStatus("pending");
        leaveRequest.setAuditStatus("pending");
        leaveRequest.setCreateTime(LocalDateTime.now());
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    @Override
    public LeaveRequestModel findById(String id) {
        return leaveRequestRepository.findById(id).orElse(null);
    }
    
    @Override
    public List<LeaveRequestModel> findByStudentId(String studentId) {
        return leaveRequestRepository.findByStudentId(studentId);
    }
    
    @Override
    public List<LeaveRequestModel> getPendingLeaveRequests() {
        return leaveRequestRepository.findByAuditStatus("pending");
    }
    
    @Override
    public LeaveRequestModel auditLeaveRequest(String id, String auditStatus, String auditComment, String auditorId, String auditorName) {
        LeaveRequestModel leaveRequest = leaveRequestRepository.findById(id).orElse(null);
        if (leaveRequest == null) {
            return null;
        }
        
        leaveRequest.setAuditStatus(auditStatus);
        leaveRequest.setAuditComment(auditComment);
        leaveRequest.setAuditorId(auditorId);
        leaveRequest.setAuditorName(auditorName);
        leaveRequest.setAuditTime(LocalDateTime.now());
        
        // 如果批准，状态设为approved，等待开始时间自动变为on_leave
        if ("approved".equals(auditStatus)) {
            leaveRequest.setStatus("approved");
        } else if ("rejected".equals(auditStatus)) {
            leaveRequest.setStatus("rejected");
        }
        
        leaveRequest.setUpdateTime(LocalDateTime.now());
        return leaveRequestRepository.save(leaveRequest);
    }
    
    @Override
    public LeaveRequestModel checkIn(String id, String checkInComment, Double latitude, Double longitude, String address) {
        LeaveRequestModel leaveRequest = leaveRequestRepository.findById(id).orElse(null);
        if (leaveRequest == null) {
            return null;
        }
        
        // 只有状态为on_leave、pending_check_in或overdue的才能销假
        if (!"on_leave".equals(leaveRequest.getStatus()) && 
            !"pending_check_in".equals(leaveRequest.getStatus()) && 
            !"overdue".equals(leaveRequest.getStatus())) {
            throw new RuntimeException("当前状态不允许销假");
        }
        
        leaveRequest.setStatus("completed");
        leaveRequest.setCheckInTime(LocalDateTime.now());
        leaveRequest.setCheckInComment(checkInComment);
        leaveRequest.setCheckInLatitude(latitude);
        leaveRequest.setCheckInLongitude(longitude);
        leaveRequest.setCheckInAddress(address);
        leaveRequest.setUpdateTime(LocalDateTime.now());
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    @Override
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void updateLeaveStatusAutomatically() {
        LocalDate today = LocalDate.now();
        
        // 1. 将已批准且开始时间已到的请假状态改为"on_leave"（假期中）
        // 注意：只处理状态为"approved"的，不处理已销假（completed）的
        List<LeaveRequestModel> approvedLeaves = leaveRequestRepository
                .findByAuditStatusAndStatusAndStartDateLessThanEqual("approved", "approved", today);
        for (LeaveRequestModel leave : approvedLeaves) {
            // 确保不是已销假状态
            if (!"completed".equals(leave.getStatus())) {
                leave.setStatus("on_leave");
                leave.setUpdateTime(LocalDateTime.now());
                leaveRequestRepository.save(leave);
            }
        }
        
        // 2. 将假期已结束但还在结束日期后一天内的请假状态改为"pending_check_in"（待销假）
        // 结束日期当天和结束日期后一天内，状态为"待销假"
        // 注意：只处理状态为"on_leave"的，已销假（completed）的不再处理
        LocalDate yesterday = today.minusDays(1);
        List<LeaveRequestModel> onLeaveRequests = leaveRequestRepository.findByStatus("on_leave");
        for (LeaveRequestModel leave : onLeaveRequests) {
            // 如果已经销假（有checkInTime），跳过处理
            if (leave.getCheckInTime() != null || "completed".equals(leave.getStatus())) {
                continue;
            }
            if (leave.getEndDate() != null) {
                // 如果结束日期是昨天或今天，且状态还是on_leave，改为pending_check_in
                if (!leave.getEndDate().isAfter(today) && !leave.getEndDate().isBefore(yesterday)) {
                    leave.setStatus("pending_check_in");
                    leave.setUpdateTime(LocalDateTime.now());
                    leaveRequestRepository.save(leave);
                }
            }
        }
        
        // 3. 将"待销假"状态中超过结束日期后一天的改为"overdue"（逾期未销假）
        // 结束日期后超过一天（即结束日期+2天及以后），状态改为"逾期未销假"
        // 注意：只处理未销假的记录
        LocalDate twoDaysAgo = today.minusDays(2);
        List<LeaveRequestModel> pendingCheckInLeaves = leaveRequestRepository.findByStatus("pending_check_in");
        for (LeaveRequestModel leave : pendingCheckInLeaves) {
            // 如果已经销假（有checkInTime），跳过处理
            if (leave.getCheckInTime() != null || "completed".equals(leave.getStatus())) {
                continue;
            }
            if (leave.getEndDate() != null && leave.getEndDate().isBefore(twoDaysAgo)) {
                leave.setStatus("overdue");
                leave.setUpdateTime(LocalDateTime.now());
                leaveRequestRepository.save(leave);
            }
        }
        
        // 4. 将"on_leave"状态中超过结束日期后一天的改为"overdue"（逾期未销假）
        // 处理可能遗漏的情况：如果结束日期是2天前或更早，直接改为overdue
        // 注意：只处理未销假的记录
        List<LeaveRequestModel> remainingOnLeave = leaveRequestRepository.findByStatus("on_leave");
        for (LeaveRequestModel leave : remainingOnLeave) {
            // 如果已经销假（有checkInTime），跳过处理
            if (leave.getCheckInTime() != null || "completed".equals(leave.getStatus())) {
                continue;
            }
            if (leave.getEndDate() != null && leave.getEndDate().isBefore(twoDaysAgo)) {
                leave.setStatus("overdue");
                leave.setUpdateTime(LocalDateTime.now());
                leaveRequestRepository.save(leave);
            }
        }
    }
    
    @Override
    public List<LeaveRequestModel> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }
    
    @Override
    public LeaveRequestModel updateLeaveRequest(LeaveRequestModel leaveRequest) {
        leaveRequest.setUpdateTime(LocalDateTime.now());
        return leaveRequestRepository.save(leaveRequest);
    }
}

