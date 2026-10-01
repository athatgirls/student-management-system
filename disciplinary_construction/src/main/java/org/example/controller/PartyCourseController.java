package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.annotation.CurrentUser;
import org.example.model.PartyCourseModel;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.PartyCourseService;
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
    private CurrentUserAccessService currentUserAccessService;

    @Operation(summary = "添加党课记录")
    @PostMapping("/add")
    public ResponseResult<PartyCourseModel> addPartyCourse(@RequestBody PartyCourseModel partyCourse, @CurrentUser Map<String, Object> currentUser) {
        if (!currentUserAccessService.isAdmin(currentUser)) {
            partyCourse.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
            partyCourse.setStudentName((String) currentUser.get("username"));
            resetAudit(partyCourse);
        }
        PartyCourseModel result = partyCourseService.addPartyCourse(partyCourse);
        return ResponseResult.success(result);
    }

    @Operation(summary = "更新党课记录")
    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseResult<PartyCourseModel> updatePartyCourse(@RequestBody PartyCourseModel partyCourse, @CurrentUser Map<String, Object> currentUser) {
        PartyCourseModel existing = partyCourseService.getPartyCourseById(partyCourse.getId());
        if (existing != null) {
            currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        }
        if (!currentUserAccessService.isAdmin(currentUser)) {
            partyCourse.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
            partyCourse.setStudentName((String) currentUser.get("username"));
            resetAudit(partyCourse);
        }
        PartyCourseModel result = partyCourseService.updatePartyCourse(partyCourse);
        return ResponseResult.success(result);
    }

    @Operation(summary = "删除党课记录")
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseResult<String> deletePartyCourse(@PathVariable String id,
                                                    @CurrentUser Map<String, Object> currentUser) {
        PartyCourseModel existing = partyCourseService.getPartyCourseById(id);
        if (existing != null) {
            currentUserAccessService.requireStudentAccess(currentUser, existing.getStudentId());
        }
        partyCourseService.deletePartyCourse(id);
        return ResponseResult.success("删除成功");
    }

    @Operation(summary = "获取学生党课记录")
    @GetMapping("/student/{studentId}")
    public ResponseResult<List<PartyCourseModel>> getStudentPartyCourses(@PathVariable String studentId,
                                                                         @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        List<PartyCourseModel> result = partyCourseService.getStudentPartyCourses(studentId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据ID获取党课记录")
    @GetMapping("/{id}")
    public ResponseResult<PartyCourseModel> getPartyCourseById(@PathVariable String id,
                                                               @CurrentUser Map<String, Object> currentUser) {
        PartyCourseModel result = partyCourseService.getPartyCourseById(id);
        if (result != null) {
            currentUserAccessService.requireStudentAccess(currentUser, result.getStudentId());
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
        String auditorId = (String) currentUser.get("userId");
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
} 
