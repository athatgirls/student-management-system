package org.example.service.Impl;

import org.example.model.PartyApplicationModel;
import org.example.model.StudentModel;
import org.example.repository.PartyApplicationRepository;
import org.example.repository.StudentRepository;
import org.example.service.PartyApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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
    public PartyApplicationModel getByStudentId(String studentId) {
        return repository.findByStudentId(studentId).orElse(null);
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
        List<PartyApplicationModel> byStudentId = repository.findByStudentIdContaining(keyword);
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
            
            // 查找学生
            StudentModel foundStudent = null;
            if (studentId != null && !studentId.trim().isEmpty()) {
                foundStudent = studentRepository.findByStudentId(studentId.trim());
            }
            
            // 如果通过学号找不到，尝试通过姓名查找
            if (foundStudent == null && name != null && !name.trim().isEmpty()) {
                List<StudentModel> studentsByName = studentRepository.findByName(name.trim());
                if (studentsByName != null && !studentsByName.isEmpty()) {
                    if (studentId != null && !studentId.trim().isEmpty()) {
                        foundStudent = studentsByName.stream()
                                .filter(s -> studentId.trim().equals(s.getStudentId()))
                                .findFirst()
                                .orElse(studentsByName.get(0));
                    } else {
                        foundStudent = studentsByName.get(0);
                    }
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
            
            // 检查是否已经存在入党申请记录
            Optional<PartyApplicationModel> existingApplication = repository.findByStudentId(foundStudent.getStudentId());
            if (existingApplication.isPresent()) {
                // 如果已存在，更新阶段
                PartyApplicationModel application = existingApplication.get();
                if (currentStage != null && !currentStage.isEmpty()) {
                    application.setCurrentStage(currentStage);
                    application.setUpdateTime(LocalDateTime.now());
                    repository.save(application);
                }
                
                Map<String, Object> matched = new HashMap<>();
                matched.put("name", foundStudent.getName());
                matched.put("studentId", foundStudent.getStudentId());
                matched.put("action", "已更新");
                matchedResults.add(matched);
                continue;
            }
            
            // 创建新的入党申请记录
            PartyApplicationModel application = new PartyApplicationModel();
            application.setStudentId(foundStudent.getStudentId());
            application.setName(foundStudent.getName());
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
            matched.put("name", foundStudent.getName());
            matched.put("studentId", foundStudent.getStudentId());
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

