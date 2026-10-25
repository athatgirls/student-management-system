package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.VolunteerServiceModel;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.StudentRefService;
import org.example.service.VolunteerServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/volunteer-service")
@Tag(name = "志愿服务管理", description = "志愿服务相关接口")
public class VolunteerServiceController {

    @Autowired
    private VolunteerServiceService volunteerServiceService;

    @Autowired
    private StudentRefService studentRefService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Operation(summary = "添加志愿服务记录")
    @PostMapping("/add")
    public ResponseResult<VolunteerServiceModel> addVolunteerService(@RequestBody VolunteerServiceModel volunteerService, @CurrentUser Map<String, Object> currentUser) {
        if (currentUserAccessService.isAdmin(currentUser)) {
            // 管理端录入：按学号指定目标学生，姓名以学生档案为准，不信任请求体
            fillStudentInfo(volunteerService, currentUserAccessService.requireAdminTarget(currentUser, volunteerService.getStudentId()));
        } else {
            // 学生本人提交：身份一律取本人档案
            fillStudentInfo(volunteerService, studentRefService.current(currentUser));
            resetAudit(volunteerService);
        }
        VolunteerServiceModel result = volunteerServiceService.addVolunteerService(volunteerService);
        return ResponseResult.success(result);
    }

    @Operation(summary = "更新志愿服务记录")
    @PostMapping("/update")
    public ResponseResult<VolunteerServiceModel> updateVolunteerService(@RequestBody VolunteerServiceModel volunteerService, @CurrentUser Map<String, Object> currentUser) {
        VolunteerServiceModel existing = volunteerServiceService.getVolunteerServiceById(volunteerService.getId());
        if (existing != null) {
            currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        }
        if (currentUserAccessService.isAdmin(currentUser)) {
            // 管理端维护：按学号指定目标学生，姓名以学生档案为准，不信任请求体
            fillStudentInfo(volunteerService, currentUserAccessService.requireAdminTarget(currentUser, volunteerService.getStudentId()));
        } else {
            // 学生本人维护：身份一律取本人档案
            fillStudentInfo(volunteerService, studentRefService.current(currentUser));
            resetAudit(volunteerService);
        }
        VolunteerServiceModel result = volunteerServiceService.updateVolunteerService(volunteerService);
        return ResponseResult.success(result);
    }

    @Operation(summary = "删除志愿服务记录")
    @PostMapping("/delete/{id}")
    public ResponseResult<String> deleteVolunteerService(@PathVariable String id,
                                                         @CurrentUser Map<String, Object> currentUser) {
        VolunteerServiceModel existing = volunteerServiceService.getVolunteerServiceById(id);
        if (existing != null) {
            currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        }
        volunteerServiceService.deleteVolunteerService(id);
        return ResponseResult.success("删除成功");
    }

    @Operation(summary = "获取学生志愿服务记录")
    @GetMapping("/student/{studentAccountId}")
    public ResponseResult<List<VolunteerServiceModel>> getStudentVolunteerServices(@PathVariable String studentAccountId,
                                                                                    @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        List<VolunteerServiceModel> result = volunteerServiceService.getStudentVolunteerServices(studentAccountId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据ID获取志愿服务记录")
    @GetMapping("/{id}")
    public ResponseResult<VolunteerServiceModel> getVolunteerServiceById(@PathVariable String id,
                                                                         @CurrentUser Map<String, Object> currentUser) {
        VolunteerServiceModel result = volunteerServiceService.getVolunteerServiceById(id);
        if (result != null) {
            currentUserAccessService.requireStudentRecord(currentUser, result.getStudentAccountId());
        }
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取待审核志愿服务列表")
    @GetMapping("/pending")
    public ResponseResult<List<VolunteerServiceModel>> getPendingVolunteerServices() {
        List<VolunteerServiceModel> result = volunteerServiceService.getPendingVolunteerServices();
        return ResponseResult.success(result);
    }

    @Operation(summary = "审核志愿服务记录")
    @PostMapping("/audit/{id}")
    public ResponseResult<VolunteerServiceModel> auditVolunteerService(
            @PathVariable String id,
            @RequestBody Map<String, String> auditData,
            @CurrentUser Map<String, Object> currentUser) {
        String auditorId = currentUserAccessService.requireUserId(currentUser);
        String auditStatus = auditData.get("auditStatus");
        String auditComment = auditData.get("auditComment");
        
        VolunteerServiceModel result = volunteerServiceService.auditVolunteerService(id, auditStatus, auditComment, auditorId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取所有志愿服务列表")
    @GetMapping("/list")
    public ResponseResult<List<VolunteerServiceModel>> getAllVolunteerServices() {
        List<VolunteerServiceModel> result = volunteerServiceService.getAllVolunteerServices();
        return ResponseResult.success(result);
    }

    @Operation(summary = "搜索志愿服务")
    @GetMapping("/search")
    public ResponseResult<List<VolunteerServiceModel>> searchVolunteerServices(@RequestParam String keyword) {
        List<VolunteerServiceModel> result = volunteerServiceService.searchVolunteerServices(keyword);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据服务类型获取志愿服务列表")
    @GetMapping("/type/{serviceType}")
    public ResponseResult<List<VolunteerServiceModel>> getVolunteerServicesByType(@PathVariable String serviceType) {
        List<VolunteerServiceModel> result = volunteerServiceService.getVolunteerServicesByType(serviceType);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据状态获取志愿服务列表")
    @GetMapping("/status/{status}")
    public ResponseResult<List<VolunteerServiceModel>> getVolunteerServicesByStatus(@PathVariable String status) {
        List<VolunteerServiceModel> result = volunteerServiceService.getVolunteerServicesByStatus(status);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据组织获取志愿服务列表")
    @GetMapping("/organization/{organization}")
    public ResponseResult<List<VolunteerServiceModel>> getVolunteerServicesByOrganization(@PathVariable String organization) {
        List<VolunteerServiceModel> result = volunteerServiceService.getVolunteerServicesByOrganization(organization);
        return ResponseResult.success(result);
    }

    private void resetAudit(VolunteerServiceModel service) {
        service.setAuditStatus("待审核");
        service.setAuditComment(null);
        service.setAuditorId(null);
        service.setAuditTime(null);
    }

    /** 写入学生标识：studentAccountId 是关联键，studentId（学号）与姓名仅展示。 */
    private void fillStudentInfo(VolunteerServiceModel service, StudentRef ref) {
        service.setStudentAccountId(ref.getAccountId());
        service.setStudentId(ref.getStudentId());
        service.setStudentName(ref.getName());
    }
} 
