package org.example.service;

import org.example.model.PartyMemberModel;
import java.util.List;
import java.util.Map;

public interface PartyMemberService {
    // 学生操作
    PartyMemberModel addPartyMember(PartyMemberModel partyMember);
    PartyMemberModel updatePartyMember(PartyMemberModel partyMember);
    void deletePartyMember(String id);
    /** 按学生关联键（账号主键）查询，约定见 {@link org.example.dto.StudentRef}。 */
    List<PartyMemberModel> getStudentPartyMembers(String studentAccountId);
    PartyMemberModel getPartyMemberById(String id);

    // 管理员操作
    List<PartyMemberModel> getPendingPartyMembers();
    PartyMemberModel auditPartyMember(String id, String auditStatus, String auditComment, String auditorId);
    List<PartyMemberModel> getAllPartyMembers();
    List<PartyMemberModel> searchPartyMembers(String keyword);
    List<PartyMemberModel> getPartyMembersByBranch(String branch);
    List<PartyMemberModel> getPartyMembersByStatus(String isRegular);
    
    // 批量导入党员
    Map<String, Object> batchImportPartyMembers(List<Map<String, String>> students);
} 