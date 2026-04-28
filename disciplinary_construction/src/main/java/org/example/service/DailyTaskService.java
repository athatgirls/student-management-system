package org.example.service;

import org.example.model.DailyTaskModel;
import org.example.model.DailyTaskSubmissionModel;
import java.util.List;
import java.util.Map;

public interface DailyTaskService {
    // 任务相关
    DailyTaskModel createTask(DailyTaskModel task);
    List<DailyTaskModel> getAllTasks();
    List<DailyTaskModel> getActiveTasks();
    DailyTaskModel getTaskById(String id);
    void deleteTask(String id);
    
    // 提交相关
    DailyTaskSubmissionModel submitTask(DailyTaskSubmissionModel submission);
    List<DailyTaskSubmissionModel> getSubmissionsByTaskId(String taskId);
    DailyTaskSubmissionModel getSubmissionByTaskAndStudent(String taskId, String studentId);
    
    // 统计相关
    List<Map<String, Object>> getCompletionStats(String taskId);
    byte[] exportCompletionExcel(String taskId);
    
    // 任务完成度分析相关
    List<Map<String, Object>> getTaskCompletionAnalysis(String grade);
    byte[] exportTaskCompletionAnalysisExcel(String grade);
    List<Map<String, Object>> getStudentIncompleteTasks(String studentId);
}

