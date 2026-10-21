package org.example.util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.DailyTaskModel;
import java.time.*;
import java.util.*;

public final class TaskSubmissionValidation {
    private TaskSubmissionValidation() { }
    public static void validateDefinition(DailyTaskModel task) {
        SubmissionValidation.required(task.getTitle(), "任务标题");
        SubmissionValidation.required(task.getType(), "任务类型");
        if (task.getDeadline() == null || !task.getDeadline().isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))))
            throw new IllegalArgumentException("截止日期必须晚于当前时间");
        if ("registration".equals(task.getTaskCategory()) && (task.getMaxParticipants() == null || task.getMaxParticipants() < 1))
            throw new IllegalArgumentException("报名人数限制必须为正整数");
        Set<String> names = new HashSet<>();
        if (task.getFields() != null) for (DailyTaskModel.TaskField field : task.getFields()) {
            if (field == null) throw new IllegalArgumentException("字段定义不能为空");
            SubmissionValidation.required(field.getFieldName(), "字段名称");
            field.setFieldName(field.getFieldName().trim());
            if (!names.add(field.getFieldName())) throw new IllegalArgumentException("字段名称不能重复");
            if (!Arrays.asList("text", "textarea", "number", "date", "select", "file").contains(field.getFieldType()))
                throw new IllegalArgumentException("字段类型无效");
            if ("select".equals(field.getFieldType()) && (field.getOptions() == null || field.getOptions().isEmpty()
                    || field.getOptions().stream().anyMatch(value -> value == null || value.isBlank())))
                throw new IllegalArgumentException("请填写下拉字段的选项");
        }
        task.setTitle(task.getTitle().trim());
        task.setType(task.getType().trim());
    }
    public static String category(String category) {
        if (category == null || category.isBlank()) return "daily";
        if (!Arrays.asList("academic", "daily", "volunteer", "routine").contains(category)) throw new IllegalArgumentException("请选择有效的活动分类");
        return category;
    }
    public static void validate(DailyTaskModel task, String content) {
        if (!task.isActive()) throw new IllegalArgumentException("任务已停用");
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))) throw new IllegalArgumentException("任务已截止");
        if (task.getFields() == null || task.getFields().isEmpty()) return;
        JsonNode values;
        try { values = new ObjectMapper().readTree(content == null ? "{}" : content); }
        catch (Exception e) { throw new IllegalArgumentException("任务填写内容格式错误"); }
        if (values == null || !values.isObject()) throw new IllegalArgumentException("任务填写内容格式错误");
        for (DailyTaskModel.TaskField field : task.getFields()) {
            if (field.getConditionalField() != null && !field.getConditionalField().isBlank()
                    && field.getConditionalValue() != null && !field.getConditionalValue().isBlank()
                    && !field.getConditionalValue().equals(values.path(field.getConditionalField()).asText())) continue;
            String name = field.getFieldName();
            String value = values.path(name).isNull() ? "" : values.path(name).asText("");
            if (field.isRequired()) SubmissionValidation.required(value, name);
            if (value.isBlank()) continue;
            if ("select".equals(field.getFieldType()) && field.getOptions() != null && !field.getOptions().contains(value))
                throw new IllegalArgumentException(name + "选项无效");
            if ("number".equals(field.getFieldType())) {
                try { if (!Double.isFinite(Double.parseDouble(value))) throw new NumberFormatException(); }
                catch (NumberFormatException e) { throw new IllegalArgumentException(name + "应为有效数字"); }
            }
            if ("date".equals(field.getFieldType())) {
                LocalDateTime end = date(value, name);
                String start = field.getNotBeforeField();
                if (start == null || start.isBlank()) {
                    start = name.contains("返校") ? name.replace("返校", "离校") : name.contains("结束") ? name.replace("结束", "开始") : null;
                }
                if (start != null && !values.path(start).asText("").isBlank()
                        && end.isBefore(date(values.path(start).asText(), start)))
                    throw new IllegalArgumentException(name + "不能早于" + start);
            }
        }
    }
    private static LocalDateTime date(String value, String name) {
        try {
            if (value.length() == 10) return LocalDate.parse(value).atStartOfDay();
            try { return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(); }
            catch (java.time.format.DateTimeParseException ignored) { return LocalDateTime.parse(value); }
        } catch (Exception e) { throw new IllegalArgumentException(name + "不是有效日期"); }
    }
}
