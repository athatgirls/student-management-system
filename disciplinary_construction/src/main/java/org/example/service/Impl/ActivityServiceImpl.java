package org.example.service.Impl;

import org.example.model.ActivityModel;
import org.example.model.DailyTaskModel;
import org.example.model.DailyTaskSubmissionModel;
import org.example.model.StudentModel;
import org.example.repository.ActivityRepository;
import org.example.repository.DailyTaskRepository;
import org.example.repository.DailyTaskSubmissionRepository;
import org.example.repository.StudentRepository;
import org.example.service.ActivityService;
import org.example.service.DailyTaskAudienceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DailyTaskRepository taskRepository;

    @Autowired
    private DailyTaskSubmissionRepository submissionRepository;

    @Autowired
    private DailyTaskAudienceService dailyTaskAudienceService;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private org.example.service.VolunteerCompletionService volunteerCompletionService;

    @Override
    public ActivityModel createActivity(ActivityModel activity) {
        DailyTaskModel task = taskRepository.findById(activity.getTaskId()).orElseThrow(() -> new IllegalArgumentException("关联任务不存在"));
        activity.setActivityCategory(org.example.util.TaskSubmissionValidation.category(task.getActivityCategory()));
        activity.setId(null);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activity.setMatched(false);
        if (activity.getParticipantStudentIds() == null) {
            activity.setParticipantStudentIds(new ArrayList<>());
        }
        return activityRepository.save(activity);
    }

    @Override
    public List<ActivityModel> getAllActivities() {
        return activityRepository.findAllByOrderByActivityTimeDesc();
    }

    @Override
    public ActivityModel getActivityById(String id) {
        return activityRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteActivity(String id) {
        activityRepository.deleteById(id);
    }

    @Override
    public Map<String, Object> importStudentsAndMatchTasks(String activityId, List<Map<String, String>> students) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> matchedResults = new ArrayList<>();
        List<Map<String, Object>> unmatchedResults = new ArrayList<>();
        
        ActivityModel activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            result.put("success", false);
            result.put("message", "活动不存在");
            return result;
        }

        // 检查活动是否关联了任务
        if (activity.getTaskId() == null || activity.getTaskId().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "活动未关联任务，请先为活动关联一个任务");
            return result;
        }

        // 获取关联的特定任务
        DailyTaskModel associatedTask = taskRepository.findById(activity.getTaskId()).orElse(null);
        if (associatedTask == null) {
            result.put("success", false);
            result.put("message", "关联的任务不存在");
            return result;
        }

        if (!associatedTask.isActive()) {
            result.put("success", false);
            result.put("message", "关联的任务未激活");
            return result;
        }

        // 已报名者即使满员也必须允许导入到场名单；只为新增报名占用名额。
        Set<String> participantIds = new LinkedHashSet<>(activity.getParticipantStudentIds() == null ? Collections.emptyList() : activity.getParticipantStudentIds());

        for (Map<String, String> studentInfo : students) {
            String name = studentInfo.get("name");
            String studentId = studentInfo.get("studentId");
            
            // 先尝试通过学号查找
            StudentModel foundStudent = null;
            if (studentId != null && !studentId.trim().isEmpty()) {
                foundStudent = studentRepository.findByStudentId(studentId.trim());
            }
            
            // 如果通过学号找不到，尝试通过姓名查找
            if (foundStudent == null && (studentId == null || studentId.trim().isEmpty()) && name != null && !name.trim().isEmpty()) {
                List<StudentModel> studentsByName = studentRepository.findByName(name.trim());
                if (studentsByName != null && studentsByName.size() == 1) {
                    foundStudent = studentsByName.get(0);
                }
            }

            if (foundStudent == null) {
                Map<String, Object> unmatched = new HashMap<>();
                unmatched.put("name", name);
                unmatched.put("studentId", studentId);
                unmatched.put("reason", "未找到匹配的学生");
                unmatchedResults.add(unmatched);
                continue;
            }

            final StudentModel student = foundStudent;

            // 活动导入与学生任务列表、任务提交使用同一套接收范围判断。
            boolean canCompleteTask = dailyTaskAudienceService.matches(associatedTask, student);
            String restrictionReason = canCompleteTask ? null : "不符合任务接收范围";

            int matchedCount = 0;
            if (canCompleteTask) {
                // 检查是否已经提交过
                Optional<DailyTaskSubmissionModel> existing = submissionRepository.findByTaskIdAndStudentId(
                        associatedTask.getId(), student.getId());

                if (!existing.isPresent()) {
                    // 如果是报名型任务，需要检查报名人数限制
                    if ("registration".equals(associatedTask.getTaskCategory()) && associatedTask.getMaxParticipants() != null && associatedTask.getMaxParticipants() > 0) {
                        int maxParticipants = associatedTask.getMaxParticipants() != null ? associatedTask.getMaxParticipants() : 0;
                        int currentParticipants = associatedTask.getCurrentParticipants() != null ? associatedTask.getCurrentParticipants() : 0;
                        
                        if (currentParticipants >= maxParticipants) {
                            // 已满员
                            Map<String, Object> unmatched = new HashMap<>();
                            unmatched.put("name", student.getName());
                            unmatched.put("studentId", student.getStudentId());
                            unmatched.put("reason", "报名型任务已满员");
                            unmatchedResults.add(unmatched);
                            continue;
                        }
                        
                        // 使用原子操作更新报名人数
                        Query query = new Query(
                            Criteria.where("_id").is(associatedTask.getId())
                                .and("currentParticipants").lt(maxParticipants)
                        );
                        Update update = new Update().inc("currentParticipants", 1);
                        
                        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, DailyTaskModel.class);
                        
                        if (updateResult.getModifiedCount() == 0) {
                            // 更新失败，说明已满员
                            Map<String, Object> unmatched = new HashMap<>();
                            unmatched.put("name", student.getName());
                            unmatched.put("studentId", student.getStudentId());
                            unmatched.put("reason", "报名型任务已满员");
                            unmatchedResults.add(unmatched);
                            continue;
                        }
                    }
                    
                    // 创建新的提交记录
                    DailyTaskSubmissionModel submission = new DailyTaskSubmissionModel();
                    submission.setTaskId(associatedTask.getId());
                    submission.setStudentId(student.getId());
                    submission.setStudentName(student.getName());
                    submission.setSubmissionTime(LocalDateTime.now());
                    submission.setUpdateTime(LocalDateTime.now());
                    submission.setStatus("completed");
                    // 提交内容可以包含活动信息
                    submission.setContent(String.format("{\"activityId\":\"%s\",\"activityTitle\":\"%s\"}", 
                            activityId, activity.getTitle()));
                    
                    submissionRepository.save(submission);
                    matchedCount = 1;
                } else {
                    // 已经提交过，也算匹配成功
                    matchedCount = 1;
                }
                volunteerCompletionService.sync(associatedTask, student, activity);
                participantIds.add(student.getId());
            } else {
                // 不符合任务限制条件
                Map<String, Object> unmatched = new HashMap<>();
                unmatched.put("name", student.getName());
                unmatched.put("studentId", student.getStudentId());
                unmatched.put("reason", restrictionReason);
                unmatchedResults.add(unmatched);
                continue;
            }

            Map<String, Object> matched = new HashMap<>();
            matched.put("name", student.getName());
            matched.put("studentId", student.getStudentId());
            matched.put("matchedTaskCount", matchedCount);
            matchedResults.add(matched);
        }

        // 更新活动的参与学生列表
        activity.setParticipantStudentIds(new ArrayList<>(participantIds));
        activity.setMatched(true);
        activity.setUpdateTime(LocalDateTime.now());
        activityRepository.save(activity);

        result.put("success", true);
        result.put("matchedCount", matchedResults.size());
        result.put("unmatchedCount", unmatchedResults.size());
        result.put("matchedResults", matchedResults);
        result.put("unmatchedResults", unmatchedResults);

        return result;
    }
}
