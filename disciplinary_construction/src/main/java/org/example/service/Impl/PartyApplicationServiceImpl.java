package org.example.service.Impl;

import org.example.dto.StudentRef;
import org.example.model.PartyApplicationModel;
import org.example.model.StudentModel;
import org.example.repository.PartyApplicationRepository;
import org.example.repository.StudentRepository;
import org.example.service.PartyApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartyApplicationServiceImpl implements PartyApplicationService {

    @Autowired
    private PartyApplicationRepository repository;
    
    @Autowired
    private StudentRepository studentRepository;

    @Override
    public PartyApplicationModel addPartyApplication(PartyApplicationModel application) {
        application.setId(null);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        application.setAuditStatus("待审核");
        if (application.getCurrentStage() == null || application.getCurrentStage().isEmpty()) {
            application.setCurrentStage("提交申请书");
        }
        return repository.save(application);
    }

    @Override
    public PartyApplicationModel updatePartyApplication(PartyApplicationModel application) {
        application.setUpdateTime(LocalDateTime.now());
        return repository.save(application);
    }

    @Override
    public void deletePartyApplication(String id) {
        repository.deleteById(id);
    }

    @Override
    public PartyApplicationModel getByStudentAccountId(String studentAccountId) {
        return repository.findByStudentAccountId(studentAccountId).orElse(null);
    }

    @Override
    public PartyApplicationModel getById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<PartyApplicationModel> getAll() {
        return repository.findAll();
    }

    @Override
    public List<PartyApplicationModel> getByStage(String stage) {
        return repository.findByCurrentStage(stage);
    }

    @Override
    public PartyApplicationModel auditPartyApplication(String id, String auditStatus, String auditComment, String auditorId) {
        PartyApplicationModel application = getById(id);
        if (application != null) {
            application.setAuditStatus(auditStatus);
            application.setAuditComment(auditComment);
            application.setAuditorId(auditorId);
            application.setAuditTime(LocalDateTime.now());
            application.setUpdateTime(LocalDateTime.now());
            return repository.save(application);
        }
        return null;
    }

    @Override
    public List<PartyApplicationModel> searchPartyApplications(String keyword) {
        // 学号只是展示字段，按学号检索属于展示匹配，不走关联键
        List<PartyApplicationModel> byStudentId = repository.findAll().stream()
                .filter(application -> application.getStudentId() != null && application.getStudentId().contains(keyword))
                .collect(Collectors.toList());
        List<PartyApplicationModel> byName = repository.findByNameContaining(keyword);
        byStudentId.addAll(byName);
        return byStudentId;
    }
    
    @Override
    public Map<String, Object> batchImportPartyApplications(List<Map<String, String>> students, String currentStage) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> matchedResults = new ArrayList<>();
        List<Map<String, Object>> unmatchedResults = new ArrayList<>();
        
        for (Map<String, String> studentData : students) {
            String studentId = studentData.get("studentId");
            String name = studentData.get("name");
            
            // 按学号匹配学生（导入匹配），姓名不参与匹配
            StudentModel foundStudent = null;
            if (studentId != null && !studentId.trim().isEmpty()) {
                foundStudent = studentRepository.findByStudentId(studentId.trim());
            }
            
            if (foundStudent == null) {
                Map<String, Object> unmatched = new HashMap<>();
                unmatched.put("name", name);
                unmatched.put("studentId", studentId);
                unmatched.put("reason", "未找到匹配的学生");
                unmatchedResults.add(unmatched);
                continue;
            }
            
            // 学生身份：账号主键是关联键，学号仅展示
            StudentRef ref = new StudentRef(foundStudent.getId(), foundStudent.getStudentId(), foundStudent.getName());
            
            // 检查是否已经存在入党申请记录（按学生关联键查询）
            Optional<PartyApplicationModel> existingApplication = repository.findByStudentAccountId(ref.getAccountId());
            if (existingApplication.isPresent()) {
                // 如果已存在，更新阶段
                PartyApplicationModel application = existingApplication.get();
                if (currentStage != null && !currentStage.isEmpty()) {
                    application.setCurrentStage(currentStage);
                    application.setUpdateTime(LocalDateTime.now());
                    repository.save(application);
                }
                
                Map<String, Object> matched = new HashMap<>();
                matched.put("name", ref.getName());
                matched.put("studentId", ref.getStudentId());
                matched.put("action", "已更新");
                matchedResults.add(matched);
                continue;
            }
            
            // 创建新的入党申请记录
            PartyApplicationModel application = new PartyApplicationModel();
            application.setStudentAccountId(ref.getAccountId());
            application.setStudentId(ref.getStudentId());
            application.setName(ref.getName());
            application.setCurrentStage(currentStage != null && !currentStage.isEmpty() ? currentStage : "提交申请书");
            application.setCreateTime(LocalDateTime.now());
            application.setUpdateTime(LocalDateTime.now());
            application.setAuditStatus("待审核");
            
            // 设置其他字段（如果提供）
            if (studentData.containsKey("branch")) {
                application.setBranch(studentData.get("branch"));
            }
            if (studentData.containsKey("applicationDate")) {
                application.setApplicationDate(studentData.get("applicationDate"));
            }
            
            repository.save(application);
            
            Map<String, Object> matched = new HashMap<>();
            matched.put("name", ref.getName());
            matched.put("studentId", ref.getStudentId());
            matched.put("action", "已创建");
            matchedResults.add(matched);
        }
        
        result.put("matchedCount", matchedResults.size());
        result.put("unmatchedCount", unmatchedResults.size());
        result.put("matchedResults", matchedResults);
        result.put("unmatchedResults", unmatchedResults);
        
        return result;
    }
}

