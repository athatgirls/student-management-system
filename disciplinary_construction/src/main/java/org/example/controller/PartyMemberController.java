package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.PartyMemberModel;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.PartyMemberService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/msi/party-member")
@Tag(name = "党员管理", description = "党员相关接口")
public class PartyMemberController {

    @Autowired
    private PartyMemberService partyMemberService;

    @Autowired
    private StudentRefService studentRefService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Operation(summary = "添加党员信息")
    @PostMapping("/add")
    public ResponseResult<PartyMemberModel> addPartyMember(@RequestBody PartyMemberModel partyMember, @CurrentUser Map<String, Object> currentUser) {
        if (currentUserAccessService.isAdmin(currentUser)) {
            // 管理端录入：按学号指定目标学生，姓名以学生档案为准，不信任请求体
            fillStudentInfo(partyMember, currentUserAccessService.requireAdminTarget(currentUser, partyMember.getStudentId()));
        } else {
            // 学生本人提交：身份一律取本人档案
            fillStudentInfo(partyMember, studentRefService.current(currentUser));
            resetAudit(partyMember);
        }
        PartyMemberModel result = partyMemberService.addPartyMember(partyMember);
        return ResponseResult.success(result);
    }

    @Operation(summary = "更新党员信息")
    @PostMapping("/update")
    public ResponseResult<PartyMemberModel> updatePartyMember(@RequestBody PartyMemberModel partyMember, @CurrentUser Map<String, Object> currentUser) {
        PartyMemberModel existing = partyMemberService.getPartyMemberById(partyMember.getId());
        if (existing != null) {
            currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        }
        if (currentUserAccessService.isAdmin(currentUser)) {
            // 管理端维护：按学号指定目标学生，姓名以学生档案为准，不信任请求体
            fillStudentInfo(partyMember, currentUserAccessService.requireAdminTarget(currentUser, partyMember.getStudentId()));
        } else {
            // 学生本人维护：身份一律取本人档案
            fillStudentInfo(partyMember, studentRefService.current(currentUser));
            resetAudit(partyMember);
        }
        PartyMemberModel result = partyMemberService.updatePartyMember(partyMember);
        return ResponseResult.success(result);
    }

    @Operation(summary = "删除党员信息")
    @PostMapping("/delete/{id}")
    public ResponseResult<String> deletePartyMember(@PathVariable String id,
                                                    @CurrentUser Map<String, Object> currentUser) {
        PartyMemberModel existing = partyMemberService.getPartyMemberById(id);
        if (existing != null) {
            currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        }
        partyMemberService.deletePartyMember(id);
        return ResponseResult.success("删除成功");
    }

    @Operation(summary = "获取学生党员信息")
    @GetMapping("/student/{studentAccountId}")
    public ResponseResult<List<PartyMemberModel>> getStudentPartyMembers(@PathVariable String studentAccountId,
                                                                         @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        List<PartyMemberModel> result = partyMemberService.getStudentPartyMembers(studentAccountId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据ID获取党员信息")
    @GetMapping("/{id}")
    public ResponseResult<PartyMemberModel> getPartyMemberById(@PathVariable String id,
                                                               @CurrentUser Map<String, Object> currentUser) {
        PartyMemberModel result = partyMemberService.getPartyMemberById(id);
        if (result != null) {
            currentUserAccessService.requireStudentRecord(currentUser, result.getStudentAccountId());
        }
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取待审核党员列表")
    @GetMapping("/pending")
    public ResponseResult<List<PartyMemberModel>> getPendingPartyMembers() {
        List<PartyMemberModel> result = partyMemberService.getPendingPartyMembers();
        return ResponseResult.success(result);
    }

    @Operation(summary = "审核党员信息")
    @PostMapping("/audit/{id}")
    public ResponseResult<PartyMemberModel> auditPartyMember(
            @PathVariable String id,
            @RequestBody Map<String, String> auditData,
            @CurrentUser Map<String, Object> currentUser) {
        String auditorId = currentUserAccessService.requireUserId(currentUser);
        String auditStatus = auditData.get("auditStatus");
        String auditComment = auditData.get("auditComment");
        
        PartyMemberModel result = partyMemberService.auditPartyMember(id, auditStatus, auditComment, auditorId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取所有党员列表")
    @GetMapping("/list")
    public ResponseResult<List<PartyMemberModel>> getAllPartyMembers() {
        List<PartyMemberModel> result = partyMemberService.getAllPartyMembers();
        return ResponseResult.success(result);
    }

    @Operation(summary = "搜索党员")
    @GetMapping("/search")
    public ResponseResult<List<PartyMemberModel>> searchPartyMembers(@RequestParam String keyword) {
        List<PartyMemberModel> result = partyMemberService.searchPartyMembers(keyword);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据党支部获取党员列表")
    @GetMapping("/branch/{branch}")
    public ResponseResult<List<PartyMemberModel>> getPartyMembersByBranch(@PathVariable String branch) {
        List<PartyMemberModel> result = partyMemberService.getPartyMembersByBranch(branch);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据党员状态获取列表")
    @GetMapping("/status/{isRegular}")
    public ResponseResult<List<PartyMemberModel>> getPartyMembersByStatus(@PathVariable String isRegular) {
        List<PartyMemberModel> result = partyMemberService.getPartyMembersByStatus(isRegular);
        return ResponseResult.success(result);
    }
    
    @Operation(summary = "批量导入党员")
    @PostMapping("/batch-import")
    public ResponseResult<Map<String, Object>> batchImportPartyMembers(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> students = (List<Map<String, String>>) request.get("students");
        Map<String, Object> result = partyMemberService.batchImportPartyMembers(students);
        return ResponseResult.success(result);
    }

    private void resetAudit(PartyMemberModel partyMember) {
        partyMember.setAuditStatus("待审核");
        partyMember.setAuditComment(null);
        partyMember.setAuditorId(null);
        partyMember.setAuditTime(null);
    }

    /** 写入学生标识：studentAccountId 是关联键，studentId（学号）与姓名仅展示。 */
    private void fillStudentInfo(PartyMemberModel partyMember, StudentRef ref) {
        partyMember.setStudentAccountId(ref.getAccountId());
        partyMember.setStudentId(ref.getStudentId());
        partyMember.setName(ref.getName());
    }
} 
