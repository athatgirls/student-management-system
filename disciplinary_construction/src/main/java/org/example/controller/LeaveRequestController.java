package org.example.controller;

import org.example.model.LeaveRequestModel;
import org.example.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/leave")
public class LeaveRequestController {
    
    @Autowired
    private LeaveRequestService leaveRequestService;
    
    // 学生提交请假申请
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createLeaveRequest(@RequestBody LeaveRequestModel leaveRequest) {
        Map<String, Object> result = new HashMap<>();
        try {
            LeaveRequestModel created = leaveRequestService.createLeaveRequest(leaveRequest);
            result.put("code", 200);
            result.put("data", created);
            result.put("msg", "请假申请提交成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "提交失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 学生获取自己的请假记录
    @GetMapping("/my-leaves")
    public ResponseEntity<Map<String, Object>> getMyLeaveRequests(@RequestParam(required = false) String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 如果studentId为空，尝试从请求头或其他地方获取
            if (studentId == null || studentId.isEmpty()) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "学号不能为空");
                return ResponseEntity.ok(result);
            }
            List<LeaveRequestModel> leaves = leaveRequestService.findByStudentId(studentId);
            result.put("code", 200);
            result.put("data", leaves);
            result.put("msg", "获取成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 管理员获取所有待审核的请假
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingLeaveRequests() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<LeaveRequestModel> leaves = leaveRequestService.getPendingLeaveRequests();
            result.put("code", 200);
            result.put("data", leaves);
            result.put("msg", "获取成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 管理员获取所有请假记录
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllLeaveRequests() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<LeaveRequestModel> leaves = leaveRequestService.getAllLeaveRequests();
            result.put("code", 200);
            result.put("data", leaves);
            result.put("msg", "获取成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 管理员审核请假
    @PutMapping("/audit/{id}")
    public ResponseEntity<Map<String, Object>> auditLeaveRequest(
            @PathVariable String id,
            @RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String auditStatus = (String) requestBody.get("auditStatus");
            String auditComment = (String) requestBody.get("auditComment");
            String auditorId = (String) requestBody.get("auditorId");
            String auditorName = (String) requestBody.get("auditorName");
            
            LeaveRequestModel updated = leaveRequestService.auditLeaveRequest(id, auditStatus, auditComment, auditorId, auditorName);
            if (updated != null) {
                result.put("code", 200);
                result.put("data", updated);
                result.put("msg", "审核成功");
            } else {
                result.put("code", 404);
                result.put("msg", "请假记录不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "审核失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 学生销假
    @PutMapping("/check-in/{id}")
    public ResponseEntity<Map<String, Object>> checkIn(
            @PathVariable String id,
            @RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String checkInComment = (String) requestBody.get("checkInComment");
            Double latitude = requestBody.get("latitude") != null ? 
                Double.parseDouble(requestBody.get("latitude").toString()) : null;
            Double longitude = requestBody.get("longitude") != null ? 
                Double.parseDouble(requestBody.get("longitude").toString()) : null;
            String address = (String) requestBody.get("address");
            LeaveRequestModel updated = leaveRequestService.checkIn(id, checkInComment, latitude, longitude, address);
            if (updated != null) {
                result.put("code", 200);
                result.put("data", updated);
                result.put("msg", "销假成功");
            } else {
                result.put("code", 404);
                result.put("msg", "请假记录不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "销假失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 管理员手动更新请假状态
    @PutMapping("/update-status/{id}")
    public ResponseEntity<Map<String, Object>> updateLeaveStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = (String) requestBody.get("status");
            if (status == null || status.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "状态不能为空");
                return ResponseEntity.ok(result);
            }
            
            LeaveRequestModel leaveRequest = leaveRequestService.findById(id);
            if (leaveRequest == null) {
                result.put("code", 404);
                result.put("msg", "请假记录不存在");
                return ResponseEntity.ok(result);
            }
            
            leaveRequest.setStatus(status);
            leaveRequest.setUpdateTime(java.time.LocalDateTime.now());
            leaveRequestService.updateLeaveRequest(leaveRequest);
            
            result.put("code", 200);
            result.put("data", leaveRequest);
            result.put("msg", "状态更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "状态更新失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 手动触发状态更新（用于测试或手动执行）
    @PostMapping("/update-status")
    public ResponseEntity<Map<String, Object>> updateStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            leaveRequestService.updateLeaveStatusAutomatically();
            result.put("code", 200);
            result.put("msg", "状态更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "状态更新失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}

