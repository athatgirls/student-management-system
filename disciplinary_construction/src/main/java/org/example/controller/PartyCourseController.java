package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.PartyCourseModel;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.PartyCourseService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/party-course")
@Tag(name = "党课管理", description = "党课相关接口")
public class PartyCourseController {

    @Autowired
    private PartyCourseService partyCourseService;

    @Autowired
    private StudentRefService studentRefService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Operation(summary = "添加党课记录")
    @PostMapping("/add")
    public ResponseResult<PartyCourseModel> addPartyCourse(@RequestBody PartyCourseModel partyCourse, @CurrentUser Map<String, Object> currentUser) {
        if (currentUserAccessService.isAdmin(currentUser)) {
            // 管理端录入：按学号指定目标学生，姓名以学生档案为准，不信任请求体
            fillStudentInfo(partyCourse, currentUserAccessService.requireAdminTarget(currentUser, partyCourse.getStudentId()));
        } else {
            // 学生本人提交：身份一律取本人档案
            fillStudentInfo(partyCourse, studentRefService.current(currentUser));
            resetAudit(partyCourse);
        }
        PartyCourseModel result = partyCourseService.addPartyCourse(partyCourse);
        return ResponseResult.success(result);
    }

    @Operation(summary = "更新党课记录")
    @PostMapping("/update")
    public ResponseResult<PartyCourseModel> updatePartyCourse(@RequestBody PartyCourseModel partyCourse, @CurrentUser Map<String, Object> currentUser) {
        PartyCourseModel existing = partyCourseService.getPartyCourseById(partyCourse.getId());
        if (existing != null) {
            currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        }
        if (currentUserAccessService.isAdmin(currentUser)) {
            // 管理端维护：按学号指定目标学生，姓名以学生档案为准，不信任请求体
            fillStudentInfo(partyCourse, currentUserAccessService.requireAdminTarget(currentUser, partyCourse.getStudentId()));
        } else {
            // 学生本人维护：身份一律取本人档案
            fillStudentInfo(partyCourse, studentRefService.current(currentUser));
            resetAudit(partyCourse);
        }
        PartyCourseModel result = partyCourseService.updatePartyCourse(partyCourse);
        return ResponseResult.success(result);
    }

    @Operation(summary = "删除党课记录")
    @PostMapping("/delete/{id}")
    public ResponseResult<String> deletePartyCourse(@PathVariable String id,
                                                    @CurrentUser Map<String, Object> currentUser) {
        PartyCourseModel existing = partyCourseService.getPartyCourseById(id);
        if (existing != null) {
            currentUserAccessService.requireStudentRecord(currentUser, existing.getStudentAccountId());
        }
        partyCourseService.deletePartyCourse(id);
        return ResponseResult.success("删除成功");
    }

    @Operation(summary = "获取学生党课记录")
    @GetMapping("/student/{studentAccountId}")
    public ResponseResult<List<PartyCourseModel>> getStudentPartyCourses(@PathVariable String studentAccountId,
                                                                         @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        List<PartyCourseModel> result = partyCourseService.getStudentPartyCourses(studentAccountId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据ID获取党课记录")
    @GetMapping("/{id}")
    public ResponseResult<PartyCourseModel> getPartyCourseById(@PathVariable String id,
                                                               @CurrentUser Map<String, Object> currentUser) {
        PartyCourseModel result = partyCourseService.getPartyCourseById(id);
        if (result != null) {
            currentUserAccessService.requireStudentRecord(currentUser, result.getStudentAccountId());
        }
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取待审核党课列表")
    @GetMapping("/pending")
    public ResponseResult<List<PartyCourseModel>> getPendingPartyCourses() {
        List<PartyCourseModel> result = partyCourseService.getPendingPartyCourses();
        return ResponseResult.success(result);
    }

    @Operation(summary = "审核党课记录")
    @PostMapping("/audit/{id}")
    public ResponseResult<PartyCourseModel> auditPartyCourse(
            @PathVariable String id,
            @RequestBody Map<String, String> auditData,
            @CurrentUser Map<String, Object> currentUser) {
        String auditorId = currentUserAccessService.requireUserId(currentUser);
        String auditStatus = auditData.get("auditStatus");
        String auditComment = auditData.get("auditComment");
        
        PartyCourseModel result = partyCourseService.auditPartyCourse(id, auditStatus, auditComment, auditorId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取所有党课列表")
    @GetMapping("/list")
    public ResponseResult<List<PartyCourseModel>> getAllPartyCourses() {
        List<PartyCourseModel> result = partyCourseService.getAllPartyCourses();
        return ResponseResult.success(result);
    }

    @Operation(summary = "搜索党课")
    @GetMapping("/search")
    public ResponseResult<List<PartyCourseModel>> searchPartyCourses(@RequestParam String keyword) {
        List<PartyCourseModel> result = partyCourseService.searchPartyCourses(keyword);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据课程类型获取党课列表")
    @GetMapping("/type/{courseType}")
    public ResponseResult<List<PartyCourseModel>> getPartyCoursesByType(@PathVariable String courseType) {
        List<PartyCourseModel> result = partyCourseService.getPartyCoursesByType(courseType);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据课程状态获取党课列表")
    @GetMapping("/status/{status}")
    public ResponseResult<List<PartyCourseModel>> getPartyCoursesByStatus(@PathVariable String status) {
        List<PartyCourseModel> result = partyCourseService.getPartyCoursesByStatus(status);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据授课教师获取党课列表")
    @GetMapping("/instructor/{instructor}")
    public ResponseResult<List<PartyCourseModel>> getPartyCoursesByInstructor(@PathVariable String instructor) {
        List<PartyCourseModel> result = partyCourseService.getPartyCoursesByInstructor(instructor);
        return ResponseResult.success(result);
    }

    private void resetAudit(PartyCourseModel course) {
        course.setAuditStatus("待审核");
        course.setAuditComment(null);
        course.setAuditorId(null);
        course.setAuditTime(null);
    }

    /** 写入学生标识：studentAccountId 是关联键，studentId（学号）与姓名仅展示。 */
    private void fillStudentInfo(PartyCourseModel course, StudentRef ref) {
        course.setStudentAccountId(ref.getAccountId());
        course.setStudentId(ref.getStudentId());
        course.setStudentName(ref.getName());
    }
} 
