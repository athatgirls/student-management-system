package org.example.service;

import org.example.model.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/** Deterministic key and insert-only fields make repeated imports/submissions idempotent. */
@Service
public class VolunteerCompletionService {
    private final MongoTemplate mongo;
    public VolunteerCompletionService(MongoTemplate mongo) { this.mongo = mongo; }
    public void sync(DailyTaskModel task, StudentModel student, ActivityModel activity) {
        if (!"volunteer".equals(task.getActivityCategory())) return;
        // Signing up alone is not completion: registration activities require attendance import.
        if (activity == null && "registration".equals(task.getTaskCategory())) return;
        String id = "task:" + task.getId() + ":student:" + student.getId();
        LocalDateTime now = LocalDateTime.now();
        Update update = new Update().setOnInsert("studentId", student.getStudentId())
                .setOnInsert("studentName", student.getName()).setOnInsert("serviceName", task.getTitle())
                .setOnInsert("serviceDescription", task.getDescription()).setOnInsert("sourceTaskId", task.getId())
                .setOnInsert("serviceType", "志愿活动").setOnInsert("createTime", now)
                .setOnInsert("status", "已完成").set("updateTime", now);
        if (activity != null) {
            update.set("serviceDate", activity.getActivityTime() == null ? now.toLocalDate().toString() : activity.getActivityTime().toLocalDate().toString())
                    .set("serviceLocation", activity.getLocation()).set("auditStatus", "通过")
                    .set("auditComment", "活动参与名单确认").set("auditorId", activity.getCreatorId()).set("auditTime", now);
        } else {
            update.setOnInsert("serviceDate", now.toLocalDate().toString()).setOnInsert("auditStatus", "待审核");
        }
        mongo.upsert(Query.query(Criteria.where("_id").is(id)), update, VolunteerServiceModel.class);
    }
}
