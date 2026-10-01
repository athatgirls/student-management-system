package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.example.model.StudentModel;
import java.util.Date;

/** Explicit allowlist: credentials and institution-controlled identity are not writable here. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileUpdate {
    private String gender, nation, phone, email, nativePlace, dormitory, emergencyContact;
    private String maritalStatus, researchDirection, introduction;
    private String major, className, politicalStatus, supervisor;
    private Date birthDate;

    public StudentModel toStudent(String userId) {
        if (politicalStatus != null && !politicalStatus.isBlank()
                && !org.example.service.DailyTaskAudienceService.SUPPORTED_IDENTITIES.contains(politicalStatus)) {
            throw new IllegalArgumentException("请选择有效的政治面貌");
        }
        StudentModel model = new StudentModel();
        model.setId(userId);
        model.setGender(gender); model.setNation(nation); model.setPhone(phone); model.setEmail(email);
        model.setNativePlace(nativePlace); model.setDormitory(dormitory); model.setEmergencyContact(emergencyContact);
        model.setMaritalStatus(maritalStatus); model.setResearchDirection(researchDirection);
        model.setIntroduction(introduction); model.setBirthDate(birthDate);
        model.setMajor(major); model.setClassName(className);
        model.setPoliticalStatus(politicalStatus); model.setSupervisor(supervisor);
        return model;
    }
}
