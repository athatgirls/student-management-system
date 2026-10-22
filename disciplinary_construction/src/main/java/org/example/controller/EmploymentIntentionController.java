package org.example.controller;
import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.*;
import org.example.repository.EmploymentIntentionRepository;
import org.example.service.StudentRefService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
@RestController
@RequestMapping("/msi/employment-intentions")
public class EmploymentIntentionController {
    private final EmploymentIntentionRepository repository;
    private final MongoTemplate mongoTemplate;
    private final StudentRefService studentRefService;
    public EmploymentIntentionController(EmploymentIntentionRepository repository, MongoTemplate mongoTemplate, StudentRefService studentRefService) {
        this.repository = repository; this.mongoTemplate = mongoTemplate; this.studentRefService = studentRefService;
    }
    @GetMapping("/me")
    public EmploymentIntentionModel me(@CurrentUser Map<String,Object> user) {
        // 每个学生至多一条就业意向，按 studentAccountId（账号主键）查询
        EmploymentIntentionModel model = mongoTemplate.findOne(byStudentAccountId(studentRefService.current(user).getAccountId()), EmploymentIntentionModel.class);
        return model != null ? model : new EmploymentIntentionModel();
    }
    @PostMapping("/me")
    public EmploymentIntentionModel save(@RequestBody EmploymentIntentionModel input, @CurrentUser Map<String,Object> user) {
        StudentRef ref = studentRefService.current(user);
        if (input.getIntentionType() == null || input.getIntentionType().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择意向方向");
        // _id 独立生成，学生关联键只写 studentAccountId，学号姓名仅展示
        Query query = byStudentAccountId(ref.getAccountId());
        Update update = new Update()
                .set("studentAccountId", ref.getAccountId())
                .set("studentId", ref.getStudentId())
                .set("studentName", ref.getName())
                .set("intentionType", input.getIntentionType())
                .set("targetCity", input.getTargetCity())
                .set("targetIndustry", input.getTargetIndustry())
                .set("expectedSalary", input.getExpectedSalary())
                .set("targetPosition", input.getTargetPosition())
                .set("updateTime", LocalDateTime.now());
        FindAndModifyOptions upsertNew = FindAndModifyOptions.options().upsert(true).returnNew(true);
        try {
            // upsert：存在则更新、不存在则新建（唯一性由 studentAccountId 唯一索引保证）
            return mongoTemplate.findAndModify(query, update, upsertNew, EmploymentIntentionModel.class);
        } catch (DuplicateKeyException e) {
            // 并发首建撞唯一索引：记录已被另一请求建出，重试更新已存在记录
            EmploymentIntentionModel saved = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), EmploymentIntentionModel.class);
            return saved != null ? saved : mongoTemplate.findAndModify(query, update, upsertNew, EmploymentIntentionModel.class);
        }
    }
    @GetMapping("/admin/list")
    public List<EmploymentIntentionModel> list() { return repository.findAll(); }
    private Query byStudentAccountId(String studentAccountId) {
        return Query.query(Criteria.where("studentAccountId").is(studentAccountId));
    }
}
