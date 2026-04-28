package org.example.service.Impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.example.model.DailyTaskModel;
import org.example.model.DailyTaskSubmissionModel;
import org.example.model.StudentModel;
import org.example.model.PartyApplicationModel;
import org.example.repository.DailyTaskRepository;
import org.example.repository.DailyTaskSubmissionRepository;
import org.example.repository.StudentRepository;
import org.example.service.DailyTaskService;
import org.example.service.PartyApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DailyTaskServiceImpl implements DailyTaskService {

    @Autowired
    private DailyTaskRepository taskRepository;

    @Autowired
    private DailyTaskSubmissionRepository submissionRepository;

    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private PartyApplicationService partyApplicationService;

    @Override
    public DailyTaskModel createTask(DailyTaskModel task) {
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        // 如果是报名型任务，初始化当前报名人数为0
        if ("registration".equals(task.getTaskCategory()) && task.getCurrentParticipants() == null) {
            task.setCurrentParticipants(0);
        }
        return taskRepository.save(task);
    }

    @Override
    public List<DailyTaskModel> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public List<DailyTaskModel> getActiveTasks() {
        return taskRepository.findByActive(true);
    }

    @Override
    public DailyTaskModel getTaskById(String id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }

    @Override
    public DailyTaskSubmissionModel submitTask(DailyTaskSubmissionModel submission) {
        // 获取任务信息
        DailyTaskModel task = taskRepository.findById(submission.getTaskId()).orElse(null);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        // 检查是否已经提交过
        Optional<DailyTaskSubmissionModel> existing = submissionRepository.findByTaskIdAndStudentId(
                submission.getTaskId(), submission.getStudentId());
        
        boolean isNewSubmission = !existing.isPresent();
        
        // 如果是报名型任务且是新报名，使用原子操作检查并更新报名人数
        if ("registration".equals(task.getTaskCategory()) && isNewSubmission) {
            int maxCount = task.getMaxParticipants() != null ? task.getMaxParticipants() : 0;
            
            if (maxCount > 0) {
                // 使用MongoDB原子操作：只有当currentParticipants < maxParticipants时才增加
                Query query = new Query(Criteria.where("_id").is(task.getId())
                        .and("currentParticipants").lt(maxCount));
                
                Update update = new Update();
                update.inc("currentParticipants", 1);
                update.set("updateTime", LocalDateTime.now());
                
                // 执行原子更新，如果更新成功，说明报名成功；如果更新失败（返回0），说明人数已满
                UpdateResult result = mongoTemplate.updateFirst(query, update, DailyTaskModel.class);
                
                if (result.getModifiedCount() == 0) {
                    // 更新失败，说明人数已满或任务不存在
                    // 重新查询任务以获取最新状态
                    DailyTaskModel latestTask = taskRepository.findById(task.getId()).orElse(null);
                    if (latestTask != null) {
                        int currentCount = latestTask.getCurrentParticipants() != null ? latestTask.getCurrentParticipants() : 0;
                        if (currentCount >= maxCount) {
                            throw new RuntimeException("报名人数已满，无法报名");
                        }
                    }
                    throw new RuntimeException("报名失败，请重试");
                }
                
                // 更新成功，重新获取任务以获取最新的报名人数
                task = taskRepository.findById(task.getId()).orElse(task);
            }
        }
        
        submission.setSubmissionTime(LocalDateTime.now());
        submission.setUpdateTime(LocalDateTime.now());
        submission.setStatus("completed");
        
        if (existing.isPresent()) {
            submission.setId(existing.get().getId());
        }
        
        DailyTaskSubmissionModel savedSubmission = submissionRepository.save(submission);
        
        return savedSubmission;
    }

    @Override
    public List<DailyTaskSubmissionModel> getSubmissionsByTaskId(String taskId) {
        return submissionRepository.findByTaskId(taskId);
    }

    @Override
    public DailyTaskSubmissionModel getSubmissionByTaskAndStudent(String taskId, String studentId) {
        return submissionRepository.findByTaskIdAndStudentId(taskId, studentId).orElse(null);
    }

    @Override
    public List<Map<String, Object>> getCompletionStats(String taskId) {
        DailyTaskModel task = taskRepository.findById(taskId).orElse(null);
        List<DailyTaskSubmissionModel> submissions = submissionRepository.findByTaskId(taskId);
        Map<String, DailyTaskSubmissionModel> submissionMap = submissions.stream()
                .collect(Collectors.toMap(DailyTaskSubmissionModel::getStudentId, s -> s));

        List<Map<String, Object>> stats = new ArrayList<>();
        
        // 如果是报名型任务，只显示已报名的人员
        if (task != null && "registration".equals(task.getTaskCategory())) {
            // 只处理已提交的学生
            for (DailyTaskSubmissionModel submission : submissions) {
                StudentModel student = studentRepository.findById(submission.getStudentId()).orElse(null);
                if (student == null) continue;
                
                Map<String, Object> row = new HashMap<>();
                row.put("studentId", student.getStudentId());
                row.put("name", student.getName());
                row.put("major", student.getMajor());
                row.put("grade", student.getGrade());
                row.put("status", "已报名");
                row.put("submissionTime", submission.getSubmissionTime());
                row.put("content", submission.getContent());
                
                // 如果任务有字段定义，解析字段化数据
                if (task.getFields() != null && !task.getFields().isEmpty()) {
                    String content = submission.getContent();
                    if (content != null && !content.isEmpty()) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> fieldData = (java.util.Map<String, Object>) mapper.readValue(content, java.util.Map.class);
                            // 将每个字段的值添加到行中
                            for (DailyTaskModel.TaskField field : task.getFields()) {
                                Object fieldValue = fieldData.get(field.getFieldName());
                                row.put("field_" + field.getFieldName(), fieldValue != null ? fieldValue : "");
                            }
                        } catch (Exception e) {
                            // 如果解析失败，保持原样
                        }
                    } else {
                        // 未填写，所有字段为空
                        for (DailyTaskModel.TaskField field : task.getFields()) {
                            row.put("field_" + field.getFieldName(), "");
                        }
                    }
                }
                stats.add(row);
            }
        } else {
            // 普通任务，根据限制条件过滤学生
            List<StudentModel> allStudents = studentRepository.findAll();
            
            // 根据任务限制条件过滤学生
            List<StudentModel> filteredStudents = allStudents.stream().filter(student -> {
                // 检查年级限制
                if (task != null && task.getAllowedGrades() != null && !task.getAllowedGrades().isEmpty()) {
                    if (student.getGrade() == null || !task.getAllowedGrades().contains(student.getGrade())) {
                        return false;
                    }
                }
                
                // 检查政治面貌限制
                if (task != null && task.getAllowedPoliticalStatuses() != null && !task.getAllowedPoliticalStatuses().isEmpty()) {
                    if (student.getPoliticalStatus() == null || !task.getAllowedPoliticalStatuses().contains(student.getPoliticalStatus())) {
                        return false;
                    }
                }
                
                // 检查入党阶段限制
                if (task != null && task.getAllowedPartyStages() != null && !task.getAllowedPartyStages().isEmpty()) {
                    String studentPartyStage = null;
                    if (student.getStudentId() != null) {
                        PartyApplicationModel partyApplication = partyApplicationService.getByStudentId(student.getStudentId());
                        if (partyApplication != null) {
                            studentPartyStage = partyApplication.getCurrentStage();
                        }
                    }
                    if (studentPartyStage == null || !task.getAllowedPartyStages().contains(studentPartyStage)) {
                        return false;
                    }
                }
                
                return true;
            }).collect(Collectors.toList());
            
            for (StudentModel student : filteredStudents) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentId", student.getStudentId());
                row.put("name", student.getName());
                row.put("major", student.getMajor());
                row.put("grade", student.getGrade());
                
                DailyTaskSubmissionModel submission = submissionMap.get(student.getId());
                if (submission != null) {
                    row.put("status", "已完成");
                    row.put("submissionTime", submission.getSubmissionTime());
                    row.put("content", submission.getContent());
                    
                    // 如果任务有字段定义，解析字段化数据
                    if (task != null && task.getFields() != null && !task.getFields().isEmpty()) {
                        String content = submission.getContent();
                        if (content != null && !content.isEmpty()) {
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> fieldData = (java.util.Map<String, Object>) mapper.readValue(content, java.util.Map.class);
                                // 将每个字段的值添加到行中
                                for (DailyTaskModel.TaskField field : task.getFields()) {
                                    Object fieldValue = fieldData.get(field.getFieldName());
                                    row.put("field_" + field.getFieldName(), fieldValue != null ? fieldValue : "");
                                }
                            } catch (Exception e) {
                                // 如果解析失败，保持原样
                            }
                        } else {
                            // 未填写，所有字段为空
                            for (DailyTaskModel.TaskField field : task.getFields()) {
                                row.put("field_" + field.getFieldName(), "");
                            }
                        }
                    }
                } else {
                    row.put("status", "未完成");
                    row.put("submissionTime", null);
                    row.put("content", null);
                    // 如果任务有字段定义，未完成的字段都为空
                    if (task != null && task.getFields() != null && !task.getFields().isEmpty()) {
                        for (DailyTaskModel.TaskField field : task.getFields()) {
                            row.put("field_" + field.getFieldName(), "");
                        }
                    }
                }
                stats.add(row);
            }
        }
        return stats;
    }

    @Override
    public byte[] exportCompletionExcel(String taskId) {
        DailyTaskModel task = taskRepository.findById(taskId).orElse(null);
        
        List<Map<String, Object>> stats = getCompletionStats(taskId);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        
        // 设置基础表头
        writer.addHeaderAlias("studentId", "学号");
        writer.addHeaderAlias("name", "姓名");
        writer.addHeaderAlias("major", "专业");
        writer.addHeaderAlias("grade", "年级");
        writer.addHeaderAlias("status", "完成状态");
        writer.addHeaderAlias("submissionTime", "提交时间");
        
        // 如果任务有字段定义，解析字段化数据并展开为列
        if (task != null && task.getFields() != null && !task.getFields().isEmpty()) {
            for (DailyTaskModel.TaskField field : task.getFields()) {
                writer.addHeaderAlias("field_" + field.getFieldName(), field.getFieldName());
            }
            
            // 解析每个提交的字段数据
            for (Map<String, Object> row : stats) {
                String content = (String) row.get("content");
                if (content != null && !content.isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map<String, Object> fieldData = mapper.readValue(content, java.util.Map.class);
                        // 将字段数据添加到行中
                        for (DailyTaskModel.TaskField field : task.getFields()) {
                            row.put("field_" + field.getFieldName(), fieldData.get(field.getFieldName()));
                        }
                    } catch (Exception e) {
                        // 如果解析失败，保持原样
                    }
                } else {
                    // 未完成的任务，字段值为空
                    for (DailyTaskModel.TaskField field : task.getFields()) {
                        row.put("field_" + field.getFieldName(), "");
                    }
                }
            }
        }
        // 注意：不导出content列，即使没有字段定义也不导出
        
        // 在写入数据前，从所有行中移除content字段
        for (Map<String, Object> row : stats) {
            row.remove("content");
        }
        
        // 写入数据
        writer.write(stats, true);
        
        // 自动调整列宽
        writer.autoSizeColumnAll();
        
        writer.flush(out);
        writer.close();
        
        return out.toByteArray();
    }
    
    @Override
    public List<Map<String, Object>> getTaskCompletionAnalysis(String grade) {
        // 获取所有普通任务（不包括报名型任务）
        List<DailyTaskModel> normalTasks = taskRepository.findAll().stream()
                .filter(task -> task.isActive() && (!"registration".equals(task.getTaskCategory())))
                .collect(Collectors.toList());
        
        // 获取所有学生，如果指定了年级则过滤
        List<StudentModel> students = grade != null && !grade.isEmpty() 
                ? studentRepository.findByGrade(grade)
                : studentRepository.findAll();
        
        // 获取所有提交记录
        List<DailyTaskSubmissionModel> allSubmissions = submissionRepository.findAll();
        Map<String, Map<String, DailyTaskSubmissionModel>> studentSubmissionsMap = new HashMap<>();
        for (DailyTaskSubmissionModel submission : allSubmissions) {
            studentSubmissionsMap.computeIfAbsent(submission.getStudentId(), k -> new HashMap<>())
                    .put(submission.getTaskId(), submission);
        }
        
        // 获取所有学生的入党申请信息（用于判断限制条件）
        Map<String, String> studentPartyStageMap = new HashMap<>();
        for (StudentModel student : students) {
            if (student.getStudentId() != null) {
                PartyApplicationModel partyApplication = partyApplicationService.getByStudentId(student.getStudentId());
                if (partyApplication != null) {
                    studentPartyStageMap.put(student.getId(), partyApplication.getCurrentStage());
                }
            }
        }
        
        List<Map<String, Object>> analysis = new ArrayList<>();
        final Map<String, String> finalPartyStageMap = studentPartyStageMap;
        
        for (StudentModel student : students) {
            // 计算该学生需要完成的任务（根据限制条件）
            List<DailyTaskModel> requiredTasks = normalTasks.stream().filter(task -> {
                // 检查年级限制
                if (task.getAllowedGrades() != null && !task.getAllowedGrades().isEmpty()) {
                    if (student.getGrade() == null || !task.getAllowedGrades().contains(student.getGrade())) {
                        return false;
                    }
                }
                
                // 检查政治面貌限制
                if (task.getAllowedPoliticalStatuses() != null && !task.getAllowedPoliticalStatuses().isEmpty()) {
                    if (student.getPoliticalStatus() == null || !task.getAllowedPoliticalStatuses().contains(student.getPoliticalStatus())) {
                        return false;
                    }
                }
                
                // 检查入党阶段限制
                if (task.getAllowedPartyStages() != null && !task.getAllowedPartyStages().isEmpty()) {
                    String studentPartyStage = finalPartyStageMap.get(student.getId());
                    if (studentPartyStage == null || !task.getAllowedPartyStages().contains(studentPartyStage)) {
                        return false;
                    }
                }
                
                return true;
            }).collect(Collectors.toList());
            
            // 计算已完成的任务
            Map<String, DailyTaskSubmissionModel> studentSubmissions = studentSubmissionsMap.getOrDefault(student.getId(), new HashMap<>());
            int completedCount = 0;
            List<Map<String, Object>> incompleteTasks = new ArrayList<>();
            
            for (DailyTaskModel task : requiredTasks) {
                DailyTaskSubmissionModel submission = studentSubmissions.get(task.getId());
                if (submission != null) {
                    completedCount++;
                } else {
                    Map<String, Object> incompleteTask = new HashMap<>();
                    incompleteTask.put("taskId", task.getId());
                    incompleteTask.put("taskTitle", task.getTitle());
                    incompleteTask.put("deadline", task.getDeadline());
                    incompleteTasks.add(incompleteTask);
                }
            }
            
            int totalRequired = requiredTasks.size();
            double completionRate = totalRequired > 0 ? (double) completedCount / totalRequired * 100 : 100.0;
            
            Map<String, Object> studentAnalysis = new HashMap<>();
            studentAnalysis.put("studentId", student.getStudentId());
            studentAnalysis.put("name", student.getName());
            studentAnalysis.put("grade", student.getGrade());
            studentAnalysis.put("major", student.getMajor());
            studentAnalysis.put("className", student.getClassName());
            studentAnalysis.put("totalRequired", totalRequired);
            studentAnalysis.put("completedCount", completedCount);
            studentAnalysis.put("incompleteCount", totalRequired - completedCount);
            studentAnalysis.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
            studentAnalysis.put("incompleteTasks", incompleteTasks);
            
            analysis.add(studentAnalysis);
        }
        
        return analysis;
    }
    
    @Override
    public byte[] exportTaskCompletionAnalysisExcel(String grade) {
        List<Map<String, Object>> analysis = getTaskCompletionAnalysis(grade);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        
        // 设置表头
        writer.addHeaderAlias("studentId", "学号");
        writer.addHeaderAlias("name", "姓名");
        writer.addHeaderAlias("grade", "年级");
        writer.addHeaderAlias("major", "专业");
        writer.addHeaderAlias("className", "班级");
        writer.addHeaderAlias("totalRequired", "需完成任务数");
        writer.addHeaderAlias("completedCount", "已完成数");
        writer.addHeaderAlias("incompleteCount", "未完成数");
        writer.addHeaderAlias("completionRate", "完成度(%)");
        
        // 写入数据
        writer.write(analysis, true);
        
        // 自动调整列宽
        writer.autoSizeColumnAll();
        
        writer.flush(out);
        writer.close();
        
        return out.toByteArray();
    }
    
    @Override
    public List<Map<String, Object>> getStudentIncompleteTasks(String studentId) {
        // studentId可能是MongoDB的_id，也可能是学号，先尝试按学号查找
        StudentModel studentByStudentId = studentRepository.findByStudentId(studentId);
        StudentModel student = studentByStudentId;
        if (student == null) {
            // 如果按学号找不到，尝试按ID查找
            student = studentRepository.findById(studentId).orElse(null);
        }
        if (student == null) {
            return new ArrayList<>();
        }
        
        final StudentModel finalStudent = student;
        final String finalStudentId = finalStudent.getId();
        
        // 获取所有普通任务
        List<DailyTaskModel> normalTasks = taskRepository.findAll().stream()
                .filter(task -> task.isActive() && (!"registration".equals(task.getTaskCategory())))
                .collect(Collectors.toList());
        
        // 获取学生的提交记录（使用MongoDB的_id）
        List<DailyTaskSubmissionModel> studentSubmissions = submissionRepository.findByStudentId(finalStudentId);
        Map<String, DailyTaskSubmissionModel> submissionMap = studentSubmissions.stream()
                .collect(Collectors.toMap(DailyTaskSubmissionModel::getTaskId, s -> s));
        
        // 获取学生的入党申请信息
        String studentPartyStage = null;
        if (finalStudent.getStudentId() != null) {
            PartyApplicationModel partyApplication = partyApplicationService.getByStudentId(finalStudent.getStudentId());
            if (partyApplication != null) {
                studentPartyStage = partyApplication.getCurrentStage();
            }
        }
        final String finalPartyStage = studentPartyStage;
        
        // 筛选该学生需要完成的任务
        List<DailyTaskModel> requiredTasks = normalTasks.stream().filter(task -> {
            // 检查年级限制
            if (task.getAllowedGrades() != null && !task.getAllowedGrades().isEmpty()) {
                if (finalStudent.getGrade() == null || !task.getAllowedGrades().contains(finalStudent.getGrade())) {
                    return false;
                }
            }
            
            // 检查政治面貌限制
            if (task.getAllowedPoliticalStatuses() != null && !task.getAllowedPoliticalStatuses().isEmpty()) {
                if (finalStudent.getPoliticalStatus() == null || !task.getAllowedPoliticalStatuses().contains(finalStudent.getPoliticalStatus())) {
                    return false;
                }
            }
            
            // 检查入党阶段限制
            if (task.getAllowedPartyStages() != null && !task.getAllowedPartyStages().isEmpty()) {
                if (finalPartyStage == null || !task.getAllowedPartyStages().contains(finalPartyStage)) {
                    return false;
                }
            }
            
            return true;
        }).collect(Collectors.toList());
        
        // 找出未完成的任务
        List<Map<String, Object>> incompleteTasks = new ArrayList<>();
        for (DailyTaskModel task : requiredTasks) {
            if (!submissionMap.containsKey(task.getId())) {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("taskId", task.getId());
                taskInfo.put("taskTitle", task.getTitle());
                taskInfo.put("description", task.getDescription());
                taskInfo.put("deadline", task.getDeadline());
                taskInfo.put("createTime", task.getCreateTime());
                incompleteTasks.add(taskInfo);
            }
        }
        
        return incompleteTasks;
    }
}

