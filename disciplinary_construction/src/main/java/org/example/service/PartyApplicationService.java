package org.example.service;

import org.example.model.PartyApplicationModel;
import java.util.List;
import java.util.Map;

public interface PartyApplicationService {
    PartyApplicationModel addPartyApplication(PartyApplicationModel application);
    PartyApplicationModel updatePartyApplication(PartyApplicationModel application);
    void deletePartyApplication(String id);
    PartyApplicationModel getByStudentId(String studentId);
    PartyApplicationModel getById(String id);
    List<PartyApplicationModel> getAll();
    List<PartyApplicationModel> getByStage(String stage);
    PartyApplicationModel auditPartyApplication(String id, String auditStatus, String auditComment, String auditorId);
    List<PartyApplicationModel> searchPartyApplications(String keyword);
    
    // 批量导入入党申请（发展中的学生）
    Map<String, Object> batchImportPartyApplications(List<Map<String, String>> students, String currentStage);
}

