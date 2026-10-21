package org.example.service;

import org.example.model.DailyTaskModel;
import org.example.model.UploadedFileModel;
import org.example.repository.DailyTaskRepository;
import org.example.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Task handouts are shared only with eligible recipients, never arbitrary student proofs. */
@Service
public class TaskAttachmentAccessService {
    private final DailyTaskRepository tasks;
    private final UploadedFileRepository files;
    private final CurrentUserAccessService access;
    private final DailyTaskAudienceService audience;
    @Value("${server.servlet.context-path:/SCSE@hbut}") private String contextPath = "/SCSE@hbut";

    public TaskAttachmentAccessService(DailyTaskRepository tasks, UploadedFileRepository files,
            CurrentUserAccessService access, DailyTaskAudienceService audience) {
        this.tasks = tasks; this.files = files; this.access = access; this.audience = audience;
    }

    public void validate(DailyTaskModel task) {
        List<String> urls = task.getAttachments();
        if (urls == null || urls.isEmpty()) return;
        if (urls.size() > 5) throw new IllegalArgumentException("最多发放 5 个文件");
        String prefix = contextPath + "/uploads/";
        for (String url : urls) {
            if (url == null || !url.startsWith(prefix)
                    || !url.substring(prefix.length()).matches("[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|pdf|doc|docx)"))
                throw new IllegalArgumentException("请上传有效的任务文件");
            UploadedFileModel file = files.findById(url.substring(prefix.length())).orElse(null);
            if (file == null || task.getCreatorId() == null || !task.getCreatorId().equals(file.getOwnerId()))
                throw new IllegalArgumentException("只能发放当前管理员上传的文件");
        }
    }

    public boolean canDownload(String filename, UploadedFileModel file, Map<String, Object> user) {
        if (file == null || user == null || !"student".equals(user.get("userType"))) return false;
        return tasks.findByActiveAndAttachmentsContaining(true, contextPath + "/uploads/" + filename).stream()
                .anyMatch(task -> Objects.equals(file.getOwnerId(), task.getCreatorId())
                        && audience.matches(task, access.requireCurrentStudent(user)));
    }

    /** Submission proofs remain private to the uploader and administrators. */
    public void validateSubmission(org.example.model.DailyTaskSubmissionModel submission) {
        String attachments = submission.getAttachments();
        if (attachments == null || attachments.isBlank()) return;
        try {
            com.fasterxml.jackson.databind.JsonNode nodes = new com.fasterxml.jackson.databind.ObjectMapper().readTree(attachments);
            if (!nodes.isArray() || nodes.size() > 5) throw new IllegalArgumentException("最多提交 5 个材料文件");
            String prefix = contextPath + "/uploads/";
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
                String url = node.path("url").asText("");
                if (!url.startsWith(prefix) || !url.substring(prefix.length()).matches("[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|pdf|doc|docx)") || !seen.add(url))
                    throw new IllegalArgumentException("请上传有效的任务材料");
                UploadedFileModel file = files.findById(url.substring(prefix.length())).orElse(null);
                // ownerId 即学生账号主键（studentAccountId），可与业务记录直接比较；禁止用学号比较（见 StudentRef 约定）
                if (file == null || submission.getStudentAccountId() == null || !submission.getStudentAccountId().equals(file.getOwnerId()))
                    throw new IllegalArgumentException("只能提交本人上传的材料");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("任务材料格式错误");
        }
    }
}
