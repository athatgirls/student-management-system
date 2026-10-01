package org.example.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;

/** Validation shared by create/update paths; never trusts browser-only checks. */
public final class SubmissionValidation {
    private SubmissionValidation() { }
    public static void required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
    }
    public static <T extends Comparable<? super T>> void dates(T start, T end) {
        if (start != null && end != null && end.compareTo(start) < 0)
            throw new IllegalArgumentException("结束时间不能早于开始时间");
    }
    public static void attachments(String value) {
        try {
            JsonNode files = new ObjectMapper().readTree(value == null ? "null" : value);
            if (!files.isArray() || files.isEmpty()) throw new IllegalArgumentException();
            for (JsonNode file : files) {
                String url = file.path("url").asText();
                required(url, "材料地址");
                if (!url.matches("^(https?://|/|uploads/)[^\\s]+$")) throw new IllegalArgumentException();
            }
        } catch (Exception e) { throw new IllegalArgumentException("请上传有效的证明材料"); }
    }
    public static void validate(CompetitionModel m) {
        attachments(m.getAttachments()); required(m.getTeamMembers(), "团队成员"); required(m.getInstructorName(), "指导老师");
    }
    public static void validate(ProjectModel m) {
        attachments(m.getAttachments()); required(m.getTeamMembers(), "团队成员"); required(m.getInstructorName(), "指导老师");
        dates(m.getStartDate(), m.getEndDate());
    }
    public static void validate(PaperModel m) { attachments(m.getAttachments()); }
    public static void validate(PatentModel m) { attachments(m.getAttachments()); dates(m.getApplicationDate(), m.getAuthorizationDate()); }
    public static void validate(PartyCourseModel m) {
        required(m.getTitle(), "微党课/推文题目"); required(m.getDescription(), "推文发布链接");
        try {
            java.net.URI uri = java.net.URI.create(m.getDescription());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
                throw new IllegalArgumentException();
        } catch (Exception e) { throw new IllegalArgumentException("请填写有效的 http/https 推文发布链接"); }
        try { java.time.LocalDate.parse(m.getCourseDate()); }
        catch (Exception e) { throw new IllegalArgumentException("请填写有效的发布日期"); }
    }
}
