package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.annotation.CurrentUser;
import org.example.model.PartyApplicationModel;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.PartyApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/msi/party-application")
@Tag(name = "申请入党管理", description = "申请入党相关接口")
public class PartyApplicationController {

    @Autowired
    private PartyApplicationService partyApplicationService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Operation(summary = "添加申请入党信息")
    @PostMapping("/add")
    public ResponseResult<PartyApplicationModel> addPartyApplication(
            @RequestBody PartyApplicationModel application,
            @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) {
            application.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
            application.setName((String) currentUser.get("username"));
            resetAudit(application);
        }
        PartyApplicationModel result = partyApplicationService.addPartyApplication(application);
        return ResponseResult.success(result);
    }

    @Operation(summary = "更新申请入党信息")
    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseResult<PartyApplicationModel> updatePartyApplication(@RequestBody PartyApplicationModel application,
                                                                        @CurrentUser Map<String, Object> currentUser) {
        PartyApplicationModel existing = partyApplicationService.getById(application.getId());
        if (existing != null) {
            currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        }
        if (!currentUserAccessService.isAdmin(currentUser)) {
            application.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
            application.setName((String) currentUser.get("username"));
            resetAudit(application);
        }
        PartyApplicationModel result = partyApplicationService.updatePartyApplication(application);
        return ResponseResult.success(result);
    }

    @Operation(summary = "删除申请入党信息")
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseResult<String> deletePartyApplication(@PathVariable String id,
                                                         @CurrentUser Map<String, Object> currentUser) {
        PartyApplicationModel existing = partyApplicationService.getById(id);
        if (existing != null) {
            currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        }
        partyApplicationService.deletePartyApplication(id);
        return ResponseResult.success("删除成功");
    }

    @Operation(summary = "根据学号获取申请入党信息")
    @GetMapping("/student/{studentId}")
    public ResponseResult<PartyApplicationModel> getByStudentId(@PathVariable String studentId,
                                                                @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        PartyApplicationModel result = partyApplicationService.getByStudentId(studentId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据ID获取申请入党信息")
    @GetMapping("/{id}")
    public ResponseResult<PartyApplicationModel> getById(@PathVariable String id,
                                                         @CurrentUser Map<String, Object> currentUser) {
        PartyApplicationModel result = partyApplicationService.getById(id);
        if (result != null) {
            currentUserAccessService.requireStudentAccess(currentUser, result.getStudentId());
        }
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取所有申请入党信息")
    @GetMapping("/list")
    public ResponseResult<List<PartyApplicationModel>> getAll() {
        List<PartyApplicationModel> result = partyApplicationService.getAll();
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据阶段获取申请入党信息")
    @GetMapping("/stage/{stage}")
    public ResponseResult<List<PartyApplicationModel>> getByStage(@PathVariable String stage) {
        List<PartyApplicationModel> result = partyApplicationService.getByStage(stage);
        return ResponseResult.success(result);
    }

    @Operation(summary = "审核申请入党信息")
    @PostMapping("/audit/{id}")
    public ResponseResult<PartyApplicationModel> auditPartyApplication(
            @PathVariable String id,
            @RequestBody Map<String, String> auditData,
            @CurrentUser Map<String, Object> currentUser) {
        String auditorId = (String) currentUser.get("userId");
        String auditStatus = auditData.get("auditStatus");
        String auditComment = auditData.get("auditComment");
        
        PartyApplicationModel result = partyApplicationService.auditPartyApplication(id, auditStatus, auditComment, auditorId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "搜索申请入党信息")
    @GetMapping("/search")
    public ResponseResult<List<PartyApplicationModel>> searchPartyApplications(@RequestParam String keyword) {
        List<PartyApplicationModel> result = partyApplicationService.searchPartyApplications(keyword);
        return ResponseResult.success(result);
    }
    
    @Operation(summary = "批量导入入党申请（发展中的学生）")
    @PostMapping("/batch-import")
    public ResponseResult<Map<String, Object>> batchImportPartyApplications(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> students = (List<Map<String, String>>) request.get("students");
        String currentStage = (String) request.get("currentStage");
        if (currentStage == null || currentStage.isEmpty()) {
            currentStage = "提交申请书";
        }
        Map<String, Object> result = partyApplicationService.batchImportPartyApplications(students, currentStage);
        return ResponseResult.success(result);
    }

    private void resetAudit(PartyApplicationModel application) {
        application.setAuditStatus("待审核");
        application.setAuditComment(null);
        application.setAuditorId(null);
        application.setAuditTime(null);
    }
}
