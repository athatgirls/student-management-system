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
    private Date birthDate;

    public StudentModel toStudent(String userId) {
        StudentModel model = new StudentModel();
        model.setId(userId);
        model.setGender(gender); model.setNation(nation); model.setPhone(phone); model.setEmail(email);
        model.setNativePlace(nativePlace); model.setDormitory(dormitory); model.setEmergencyContact(emergencyContact);
        model.setMaritalStatus(maritalStatus); model.setResearchDirection(researchDirection);
        model.setIntroduction(introduction); model.setBirthDate(birthDate);
        return model;
    }
}
