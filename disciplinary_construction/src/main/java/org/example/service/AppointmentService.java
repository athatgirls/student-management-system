package org.example.service;

import org.example.model.AppointmentModel;
import org.example.model.StudentModel;
import org.example.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {
    private final AppointmentRepository repository;
    private final CurrentUserAccessService access;
    private final org.example.repository.UploadedFileRepository files;
    @org.springframework.beans.factory.annotation.Value("${server.servlet.context-path:/SCSE@hbut}")
    private String contextPath = "/SCSE@hbut";
    public AppointmentService(AppointmentRepository repository, CurrentUserAccessService access, org.example.repository.UploadedFileRepository files) {
        this.repository = repository; this.access = access; this.files = files;
    }
    public List<AppointmentModel> list(Map<String, Object> user) {
        return access.isAdmin(user) ? repository.findAll() : repository.findByStudentDbId(access.requireCurrentStudent(user).getId());
    }
    private AppointmentModel owned(String id, Map<String, Object> user) {
        AppointmentModel record = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任职记录不存在"));
        // Only the owning student maintains records; administrators have read access.
        StudentModel student = access.requireCurrentStudent(user);
        access.requireStudentAccess(user, record.getStudentDbId());
        if (!student.getId().equals(record.getStudentDbId())) throw new org.springframework.security.access.AccessDeniedException("无权修改此记录");
        return record;
    }
    public AppointmentModel save(String id, AppointmentModel input, Map<String, Object> user) {
        StudentModel student = access.requireCurrentStudent(user);
        AppointmentModel record = id == null ? new AppointmentModel() : owned(id, user);
        if (input.getPosition() == null || input.getPosition().trim().isEmpty() || input.getPosition().length() > 120
                || input.getOrganization() == null || input.getOrganization().trim().isEmpty() || input.getOrganization().length() > 200
                || input.getStartDate() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写职务名称、任职单位和开始日期，名称不能过长");
        if (input.getEndDate() != null && input.getEndDate().isBefore(input.getStartDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束日期不能早于开始日期");
        if (input.getDescription() != null && input.getDescription().length() > 4000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "职责说明不能超过4000字");
        validateAttachments(input.getAttachments(), student.getId());
        record.setStudentDbId(student.getId()); record.setStudentId(student.getStudentId()); record.setStudentName(student.getName());
        record.setPosition(input.getPosition().trim()); record.setOrganization(input.getOrganization().trim());
        record.setStartDate(input.getStartDate()); record.setEndDate(input.getEndDate());
        record.setDescription(input.getDescription()); record.setAttachments(input.getAttachments());
        if (id == null) record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        return repository.save(record);
    }
    public void delete(String id, Map<String, Object> user) { repository.delete(owned(id, user)); }

    private void validateAttachments(String value, String ownerId) {
        if (value == null || value.isBlank()) return;
        try {
            com.fasterxml.jackson.databind.JsonNode attachments = new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);
            if (!attachments.isArray() || attachments.size() > 5) throw new IllegalArgumentException();
            String prefix = contextPath + "/uploads/";
            for (com.fasterxml.jackson.databind.JsonNode attachment : attachments) {
                String url = attachment.path("url").asText();
                if (!url.startsWith(prefix) || !url.substring(prefix.length()).matches("[a-zA-Z0-9_-]+\\.(pdf|doc|docx|png|jpg|jpeg)")) throw new IllegalArgumentException();
                org.example.model.UploadedFileModel file = files.findById(url.substring(prefix.length())).orElse(null);
                if (file == null || !ownerId.equals(file.getOwnerId())) throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择自己上传的有效证明材料（最多5个）");
        }
    }
}
