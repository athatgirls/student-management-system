package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.DailyTaskModel;
import org.example.model.DailyTaskSubmissionModel;
import org.example.model.StudentModel;
import org.example.repository.StudentRepository;
import org.example.service.CurrentUserAccessService;
import org.example.service.DailyTaskAudienceService;
import org.example.service.DailyTaskService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/daily-tasks")
public class DailyTaskController {

    @Autowired
    private DailyTaskService dailyTaskService;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private DailyTaskAudienceService dailyTaskAudienceService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Autowired
    private StudentRefService studentRefService;

    // 管理员：发布任务
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody DailyTaskModel task, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            task.setCreatorId(currentUserAccessService.requireUserId(currentUser));
            DailyTaskModel createdTask = dailyTaskService.createTask(task);
            result.put("code", 200);
            result.put("data", createdTask);
            result.put("msg", "任务发布成功");
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", "任务发布失败: " + e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "任务发布失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取所有任务（管理员）
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllTasks() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", dailyTaskService.getAllTasks());
        return ResponseEntity.ok(result);
    }

    // 获取激活的任务（学生），并包含完成状态
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveTasks(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // JWT userId 即学生账号主键（studentAccountId），见 org.example.dto.StudentRef
            String studentAccountId = currentUserAccessService.requireUserId(currentUser);
            
            // 获取当前学生信息
            StudentModel student = studentRepository.findById(studentAccountId).orElse(null);
            final String studentPartyStage = dailyTaskAudienceService.resolvePartyStage(student);
            
            List<DailyTaskModel> tasks = dailyTaskService.getActiveTasks();
            
            // 根据范围限制过滤任务
            List<DailyTaskModel> filteredTasks = tasks.stream()
                    .filter(task -> dailyTaskAudienceService.matches(task, student, studentPartyStage))
                    .collect(java.util.stream.Collectors.toList());
            
            // 为每个任务附加当前用户的完成状态
            java.util.stream.Stream<Map<String, Object>> taskStream = filteredTasks.stream().map(task -> {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("title", task.getTitle());
                taskMap.put("description", task.getDescription());
                taskMap.put("deadline", task.getDeadline());
                taskMap.put("createTime", org.example.util.UtcTimestamps.toWire(task.getCreateTime()));
                taskMap.put("active", task.isActive());
                taskMap.put("type", task.getType());
                taskMap.put("fields", task.getFields()); // 添加字段定义
                taskMap.put("taskCategory", task.getTaskCategory()); // 任务类别
                taskMap.put("activityCategory", org.example.util.TaskSubmissionValidation.category(task.getActivityCategory()));
                taskMap.put("maxParticipants", task.getMaxParticipants()); // 最大报名人数
                taskMap.put("attachments", task.getAttachments());
                
                // 计算当前报名人数（报名型任务）
                if ("registration".equals(task.getTaskCategory())) {
                    List<DailyTaskSubmissionModel> submissions = dailyTaskService.getSubmissionsByTaskId(task.getId());
                    int currentCount = submissions != null ? submissions.size() : 0;
                    taskMap.put("currentParticipants", currentCount);
                } else {
                    taskMap.put("currentParticipants", null);
                }
                
                DailyTaskSubmissionModel submission = dailyTaskService.getSubmissionByTaskAndStudent(task.getId(), studentAccountId);
                taskMap.put("completed", submission != null);
                if (submission != null) {
                    taskMap.put("submissionTime", org.example.util.UtcTimestamps.toWire(submission.getSubmissionTime()));
                }
                return taskMap;
            });
            
            result.put("code", 200);
            result.put("data", taskStream.collect(java.util.stream.Collectors.toList()));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取任务列表失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 学生：提交任务
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody DailyTaskSubmissionModel submission, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            StudentRef ref = studentRefService.current(currentUser);
            submission.setStudentAccountId(ref.getAccountId());
            submission.setStudentId(ref.getStudentId());
            submission.setStudentName(ref.getName());
            DailyTaskSubmissionModel savedSubmission = dailyTaskService.submitTask(submission);
            result.put("code", 200);
            result.put("data", savedSubmission);
            result.put("msg", "任务提交成功");
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "任务提交失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取任务完成情况统计（管理员）
    @GetMapping("/stats/{taskId}")
    public ResponseEntity<Map<String, Object>> getCompletionStats(@PathVariable String taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", dailyTaskService.getCompletionStats(taskId));
        return ResponseEntity.ok(result);
    }

    // 导出 Excel（管理员）
    @GetMapping("/export/{taskId}")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String taskId) {
        byte[] excelContent = dailyTaskService.exportCompletionExcel(taskId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "task_completion_" + taskId + ".xlsx");
        
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }

    // 删除任务（管理员）
    @PostMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        dailyTaskService.deleteTask(id);
        result.put("code", 200);
        result.put("msg", "任务删除成功");
        return ResponseEntity.ok(result);
    }
    
    // 获取学生自己的提交记录
    @GetMapping("/my-submission/{taskId}")
    public ResponseEntity<Map<String, Object>> getMySubmission(@PathVariable String taskId, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        String studentAccountId = currentUserAccessService.requireUserId(currentUser);
        DailyTaskSubmissionModel submission = dailyTaskService.getSubmissionByTaskAndStudent(taskId, studentAccountId);
        result.put("code", 200);
        result.put("data", submission);
        return ResponseEntity.ok(result);
    }
    
    // 获取任务完成度分析（管理员）
    @GetMapping("/completion-analysis")
    public ResponseEntity<Map<String, Object>> getTaskCompletionAnalysis(@RequestParam(required = false) String grade) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> analysis = dailyTaskService.getTaskCompletionAnalysis(grade);
            result.put("code", 200);
            result.put("data", analysis);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取任务完成度分析失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取当前学生自己的普通任务完成度
    @GetMapping("/my-completion")
    public ResponseEntity<Map<String, Object>> getMyTaskCompletion(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("data", dailyTaskService.getStudentTaskCompletionAnalysis(currentUserAccessService.requireUserId(currentUser)));
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    // 导出任务完成度分析Excel（管理员）
    @GetMapping("/completion-analysis/export")
    public ResponseEntity<byte[]> exportTaskCompletionAnalysis(@RequestParam(required = false) String grade) {
        byte[] excelContent = dailyTaskService.exportTaskCompletionAnalysisExcel(grade);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = grade != null && !grade.isEmpty() 
                ? "任务完成度分析_" + grade + ".xlsx"
                : "任务完成度分析_全部.xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        
        return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
    }
    
    // 获取学生未完成的任务列表（管理员）：studentId 为学号，服务内部解析为账号主键，见 org.example.dto.StudentRef
    @GetMapping("/student-incomplete-tasks/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentIncompleteTasks(@PathVariable String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> incompleteTasks = dailyTaskService.getStudentIncompleteTasks(studentId);
            result.put("code", 200);
            result.put("data", incompleteTasks);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取学生未完成任务失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
