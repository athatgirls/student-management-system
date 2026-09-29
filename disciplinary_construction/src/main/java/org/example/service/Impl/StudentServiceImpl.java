package org.example.service.Impl;


import org.example.model.StudentModel;
import org.example.model.DailyTaskModel;
import org.example.model.DailyTaskSubmissionModel;
import org.example.model.GradeModel;
import org.example.repository.StudentRepository;
import org.example.repository.DailyTaskRepository;
import org.example.repository.DailyTaskSubmissionRepository;
import org.example.repository.GradeRepository;
import org.example.service.StudentService;
import org.example.util.StudentGradePolicy;
import org.example.util.PasswordPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentRepository studentRepository;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.demo-data.student-password:Student123!}")
    private String demoStudentPassword;

    @Autowired
    private DailyTaskRepository dailyTaskRepository;
    @Autowired
    private DailyTaskSubmissionRepository dailyTaskSubmissionRepository;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Override
    public StudentModel login(String account, String password) {
        StudentModel student = studentRepository.findByStudentId(account);
        if (student == null) {
            student = studentRepository.findByIdCard(account);
        }
        if (student == null) {
            return null;
        }

        // 兼容旧数据中的“2024”写法，统一保存为“2024级”。
        String normalizedGrade = StudentGradePolicy.normalize(student.getGrade());
        if (normalizedGrade != null && normalizedGrade.matches("\\d{4}级")
                && !normalizedGrade.equals(student.getGrade())) {
            student.setGrade(normalizedGrade);
            studentRepository.save(student);
        }

        // Fixed-format initial passwords remain supported; they only allow the mandatory password-change flow.
        if (student.getPassword() == null || student.getPassword().isEmpty()
                || (Boolean.TRUE.equals(student.getPasswordChangeRequired())
                    && student.getInitialPasswordExpiresAt() != null
                    && !student.getInitialPasswordExpiresAt().after(new java.util.Date()))) return null;

        if (passwordEncoder.matches(password, student.getPassword())) {
            // Do not perform business actions before initial password replacement.
            if (Boolean.TRUE.equals(student.getPasswordChangeRequired())
                    || PasswordPolicy.legacyPassword(student.getStudentId()).equals(password)) return student;
            // 登录成功，检查并发布个人信息完善任务
            checkAndTriggerProfileTask(student);
            return student;
        }
        return null;
    }

    // 检查并触发个人信息完善任务
    private void checkAndTriggerProfileTask(StudentModel student) {
        // 调整判定标准：包含所有必填项
        boolean isIncomplete = student.getGender() == null || student.getGender().isEmpty() ||
                student.getEmail() == null || student.getEmail().isEmpty() ||
                student.getPhone() == null || student.getPhone().isEmpty() ||
                student.getDormitory() == null || student.getDormitory().isEmpty() ||
                student.getPoliticalStatus() == null || student.getPoliticalStatus().isEmpty() ||
                student.getNation() == null || student.getNation().isEmpty() ||
                student.getMajor() == null || student.getMajor().isEmpty() ||
                student.getClassName() == null || student.getClassName().isEmpty() ||
                student.getEducationType() == null || student.getEducationType().isEmpty() ||
                student.getBirthDate() == null;

        if (isIncomplete) {
            String taskTitle = "【系统】请完善个人基本信息";
            // 确保系统任务存在
            DailyTaskModel task = dailyTaskRepository.findByTitle(taskTitle).orElseGet(() -> {
                DailyTaskModel newTask = new DailyTaskModel();
                newTask.setTitle(taskTitle);
                newTask.setDescription("系统检测到您的个人资料不完整，请前往个人信息模块完善性别、宿舍、联系方式等信息。");
                newTask.setType("信息填写");
                newTask.setDeadline(java.time.LocalDateTime.now().plusMonths(1));
                newTask.setActive(true);
                newTask.setCreateTime(java.time.LocalDateTime.now());
                newTask.setUpdateTime(java.time.LocalDateTime.now());
                return dailyTaskRepository.save(newTask);
            });
            
            // 如果信息不完整，则删除之前的自动完成记录（撤销完成状态）
            dailyTaskRepository.findByTitle(taskTitle).ifPresent(t -> {
                dailyTaskSubmissionRepository.findByTaskIdAndStudentId(t.getId(), student.getId()).ifPresent(submission -> {
                    // 只删除系统自动确认的记录，防止误删学生自己填写的
                    if ("系统自动确认：个人信息已在个人中心完善。".equals(submission.getContent())) {
                        dailyTaskSubmissionRepository.delete(submission);
                    }
                });
            });
        } else {
            // 如果信息已完善，检查是否需要自动完成对应的系统任务
            autoCompleteProfileTask(student);
        }
    }

    private void autoCompleteProfileTask(StudentModel student) {
        String taskTitle = "【系统】请完善个人基本信息";
        Optional<DailyTaskModel> taskOpt = dailyTaskRepository.findByTitle(taskTitle);
        if (taskOpt.isPresent()) {
            String taskId = taskOpt.get().getId();
            // 如果还没提交过该任务的完成记录，则自动创建一个
            if (!dailyTaskSubmissionRepository.findByTaskIdAndStudentId(taskId, student.getId()).isPresent()) {
                DailyTaskSubmissionModel submission = new DailyTaskSubmissionModel();
                submission.setTaskId(taskId);
                submission.setStudentId(student.getId());
                submission.setStudentName(student.getName());
                submission.setContent("系统自动确认：个人信息已在个人中心完善。");
                submission.setSubmissionTime(java.time.LocalDateTime.now());
                submission.setUpdateTime(java.time.LocalDateTime.now());
                dailyTaskSubmissionRepository.save(submission);
            }
        }
    }

    @Override
    public List<StudentModel> findAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public StudentModel findById(String id) {
        return studentRepository.findById(id).orElse(null);
    }

    @Override
    public StudentModel createStudent(StudentModel student) {
        student.setId(null);
        student.setGrade(StudentGradePolicy.requireValid(student.getGrade()));
        student.setCreateTime(new java.util.Date());
        student.setUpdateTime(new java.util.Date());
        if (student.getPassword() == null || student.getPassword().isEmpty()) {
            student.setPassword(PasswordPolicy.legacyPassword(student.getStudentId()));
        }
        // 加密密码，固定格式初始密码不设24小时过期限制。
        if (student.getPassword() != null && !student.getPassword().isEmpty()) {
            PasswordPolicy.validate(student.getPassword());
            boolean fixedInitial = PasswordPolicy.legacyPassword(student.getStudentId()).equals(student.getPassword());
            student.setPassword(passwordEncoder.encode(student.getPassword()));
            student.setPasswordChangeRequired(true);
            student.setInitialPasswordExpiresAt(fixedInitial ? null : new java.util.Date(System.currentTimeMillis() + 86400000L));
        }
        return studentRepository.save(student);
    }

    @Override
    public StudentModel updateStudent(StudentModel student) {
        // 先从数据库获取原有的学生信息
        StudentModel existing = studentRepository.findById(student.getId()).orElse(null);
        if (existing == null) {
            return null;
        }

        // 只更新前端传过来的非空字段，保留原有的密码等敏感信息
        if (student.getName() != null) existing.setName(student.getName());
        if (student.getGender() != null) existing.setGender(student.getGender());
        if (student.getAge() != null) existing.setAge(student.getAge());
        if (student.getNation() != null) existing.setNation(student.getNation());
        if (student.getBirthDate() != null) {
            existing.setBirthDate(student.getBirthDate());
            // 后端自动根据生日计算年龄并保存，确保数据一致性
            existing.setAge(calculateAgeFromDate(student.getBirthDate()));
        }
        if (student.getMajor() != null) existing.setMajor(student.getMajor());
        if (student.getGrade() != null) {
            existing.setGrade(StudentGradePolicy.requireValid(student.getGrade()));
        }
        if (student.getClassName() != null) existing.setClassName(student.getClassName());
        if (student.getEducationType() != null) existing.setEducationType(student.getEducationType());
        if (student.getPhone() != null) existing.setPhone(student.getPhone());
        if (student.getEmail() != null) existing.setEmail(student.getEmail());
        if (student.getNativePlace() != null) existing.setNativePlace(student.getNativePlace());
        if (student.getDormitory() != null) existing.setDormitory(student.getDormitory());
        if (student.getEmergencyContact() != null) existing.setEmergencyContact(student.getEmergencyContact());
        if (student.getPoliticalStatus() != null) existing.setPoliticalStatus(student.getPoliticalStatus());
        if (student.getMaritalStatus() != null) existing.setMaritalStatus(student.getMaritalStatus());
        if (student.getSupervisor() != null) existing.setSupervisor(student.getSupervisor());
        if (student.getResearchDirection() != null) existing.setResearchDirection(student.getResearchDirection());
        if (student.getStatus() != null) {
            existing.setStatus(student.getStatus());
            // 如果状态改为"在读"，清空备注
            if ("在读".equals(student.getStatus())) {
                existing.setStatusRemark("");
            }
        }
        if (student.getStatusRemark() != null) existing.setStatusRemark(student.getStatusRemark());
        if (student.getWorkStatus() != null) existing.setWorkStatus(student.getWorkStatus());
        if (student.getIntroduction() != null) existing.setIntroduction(student.getIntroduction());

        // 如果前端传了新密码，才更新并加密密码
        if (student.getPassword() != null && !student.getPassword().isEmpty()) {
            PasswordPolicy.validate(student.getPassword());
            boolean fixedInitial = PasswordPolicy.legacyPassword(existing.getStudentId()).equals(student.getPassword());
            existing.setPassword(passwordEncoder.encode(student.getPassword()));
            existing.setPasswordChangeRequired(true);
            existing.setInitialPasswordExpiresAt(fixedInitial ? null : new java.util.Date(System.currentTimeMillis() + 86400000L));
        }

        existing.setUpdateTime(new java.util.Date());
        StudentModel savedStudent = studentRepository.save(existing);
        
        // 更新后再次检查信息完善程度
        checkAndTriggerProfileTask(savedStudent);
        
        return savedStudent;
    }

    // 工具方法：根据日期计算年龄
    private Integer calculateAgeFromDate(java.util.Date birthDate) {
        if (birthDate == null) return 0;
        java.util.Calendar birth = java.util.Calendar.getInstance();
        birth.setTime(birthDate);
        java.util.Calendar now = java.util.Calendar.getInstance();
        int age = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR);
        if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    @Override
    public boolean deleteStudent(String id) {
        try {
            studentRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public StudentModel findByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    @Override
    public List<StudentModel> findByName(String name) {
        return studentRepository.findByName(name);
    }

    @Override
    public List<StudentModel> findByMajor(String major) {
        return studentRepository.findByMajor(major);
    }

    @Override
    public List<StudentModel> findByGrade(String grade) {
        return studentRepository.findByGrade(grade);
    }

    @Override
    public Map<String, Object> importStudents(java.io.InputStream inputStream, String grade) throws Exception {
        final String selectedGrade = grade == null || grade.trim().isEmpty()
                ? null
                : StudentGradePolicy.requireValid(grade);
        cn.hutool.poi.excel.ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(inputStream);
        try {
            List<List<Object>> rows = reader.read();
            Map<String, Object> summary = new HashMap<>();
            List<String> sampleErrors = new ArrayList<>();
            summary.put("total", 0);
            summary.put("imported", 0);
            summary.put("invalid", 0);
            summary.put("duplicate", 0);
            summary.put("sampleErrors", sampleErrors);

            int headerRowIndex = findHeaderRow(rows);
            if (headerRowIndex < 0) {
                sampleErrors.add("未找到包含“学号”和“姓名”的表头行，请检查Excel模板");
                return summary;
            }

            Map<String, Integer> columns = resolveColumns(rows.get(headerRowIndex));
            if (columns.get("studentId") == null || columns.get("name") == null) {
                sampleErrors.add("表头缺少“学号”或“姓名”列（支持“学生学号”“学生姓名”等别名）");
                return summary;
            }

            int total = 0;
            int imported = 0;
            int invalid = 0;
            int duplicate = 0;
            Set<String> importedStudentIds = new HashSet<>();

            for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
                List<Object> row = rows.get(rowIndex);
                if (isBlankRow(row)) {
                    continue;
                }

                total++;
                int excelRowNumber = rowIndex + 1;
                String studentId = cellAt(row, columns.get("studentId"));
                String name = cellAt(row, columns.get("name"));
                if (studentId.isEmpty() || name.isEmpty()) {
                    invalid++;
                    addSampleError(sampleErrors, "第" + excelRowNumber + "行："
                            + (studentId.isEmpty() ? "学号为空" : "姓名为空"));
                    continue;
                }

                String rowGrade = cellAt(row, columns.get("grade"));
                String effectiveGrade = selectedGrade != null
                        ? selectedGrade
                        : StudentGradePolicy.normalize(rowGrade);
                if (effectiveGrade == null || effectiveGrade.isEmpty()) {
                    invalid++;
                    addSampleError(sampleErrors, "第" + excelRowNumber + "行：缺少年级");
                    continue;
                }

                if (!importedStudentIds.add(studentId)
                        || studentRepository.findByStudentId(studentId) != null) {
                    duplicate++;
                    addSampleError(sampleErrors, "第" + excelRowNumber + "行：学号" + studentId + "重复");
                    continue;
                }

                StudentModel student = new StudentModel();
                student.setStudentId(studentId);
                student.setName(name);
                String major = cellAt(row, columns.get("major"));
                if (!major.isEmpty()) {
                    student.setMajor(major);
                }
                String className = cellAt(row, columns.get("className"));
                if (!className.isEmpty()) {
                    student.setClassName(className);
                }
                student.setGrade(effectiveGrade);
                student.setCreateTime(new java.util.Date());
                student.setUpdateTime(new java.util.Date());
                student.setStatus("在读");
                // 保留学校现有的初始密码发放方式，首次使用必须修改。
                student.setPassword(passwordEncoder.encode(PasswordPolicy.legacyPassword(studentId)));
                student.setPasswordChangeRequired(true);
                studentRepository.save(student);
                imported++;
            }

            summary.put("total", total);
            summary.put("imported", imported);
            summary.put("invalid", invalid);
            summary.put("duplicate", duplicate);
            return summary;
        } finally {
            reader.close();
        }
    }

    private int findHeaderRow(List<List<Object>> rows) {
        int limit = Math.min(rows.size(), 5);
        int partialMatch = -1;
        for (int rowIndex = 0; rowIndex < limit; rowIndex++) {
            boolean hasStudentId = false;
            boolean hasName = false;
            for (Object cell : rows.get(rowIndex)) {
                String header = normalizeHeader(cell);
                hasStudentId |= header.contains("学号");
                hasName |= header.contains("姓名");
            }
            if (hasStudentId && hasName) {
                return rowIndex;
            }
            if (partialMatch < 0 && (hasStudentId || hasName)) {
                partialMatch = rowIndex;
            }
        }
        return partialMatch;
    }

    private Map<String, Integer> resolveColumns(List<Object> headerRow) {
        Map<String, Integer> columns = new HashMap<>();
        columns.put("studentId", findColumn(headerRow, "学号"));
        columns.put("name", findColumn(headerRow, "姓名"));
        columns.put("major", findColumn(headerRow, "专业"));
        columns.put("grade", findColumn(headerRow, "年级"));
        columns.put("className", findColumn(headerRow, "班级"));
        return columns;
    }

    private Integer findColumn(List<Object> headerRow, String keyword) {
        List<Integer> matches = new ArrayList<>();
        for (int index = 0; index < headerRow.size(); index++) {
            String header = normalizeHeader(headerRow.get(index));
            if (header.equals(keyword)) {
                return index;
            }
            if (header.contains(keyword)) {
                matches.add(index);
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean isBlankRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (Object cell : row) {
            if (!cellToString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String cellAt(List<Object> row, Integer columnIndex) {
        if (columnIndex == null || columnIndex < 0 || columnIndex >= row.size()) {
            return "";
        }
        return cellToString(row.get(columnIndex));
    }

    private String normalizeHeader(Object value) {
        return cellToString(value)
                .replace("\uFEFF", "")
                .replace("\u3000", "")
                .replaceAll("\\s+", "");
    }

    private String cellToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number) {
            try {
                return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                // Fall through to the regular string conversion below.
            }
        }
        return String.valueOf(value).trim();
    }

    private void addSampleError(List<String> sampleErrors, String message) {
        if (sampleErrors.size() < 5) {
            sampleErrors.add(message);
        }
    }

    @Override
    public Map<String, Object> findStudentsPage(int page, int size, String name, String studentId, String major, String grade) {
        Query query = new Query();
        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
        }
        if (studentId != null && !studentId.isEmpty()) {
            query.addCriteria(Criteria.where("studentId").regex(studentId, "i"));
        }
        if (major != null && !major.isEmpty()) {
            query.addCriteria(Criteria.where("major").is(major));
        }
        if (grade != null && !grade.isEmpty()) {
            query.addCriteria(Criteria.where("grade").is(grade));
        }

        long total = mongoTemplate.count(query, StudentModel.class);
        query.skip((long) (page - 1) * size).limit(size);
        List<StudentModel> list = mongoTemplate.find(query, StudentModel.class);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public List<String> getAllGrades() {
        java.util.Set<String> grades = new java.util.TreeSet<>(StudentGradePolicy.DEFAULT_GRADES);
        mongoTemplate.findDistinct(new Query(), "grade", StudentModel.class, String.class).stream()
                .map(StudentGradePolicy::normalize)
                .filter(value -> value != null && !value.isEmpty())
                .forEach(grades::add);
        gradeRepository.findAll().stream()
                .map(GradeModel::getGradeName)
                .map(StudentGradePolicy::normalize)
                .filter(value -> value != null && !value.isEmpty())
                .forEach(grades::add);
        return new java.util.ArrayList<>(grades);
    }

    @Override
    public int batchUpdateStatusByGrade(String grade, String status, String statusRemark) {
        Query query = new Query(Criteria.where("grade").is(grade));
        List<StudentModel> students = mongoTemplate.find(query, StudentModel.class);
        
        int count = 0;
        for (StudentModel student : students) {
            student.setStatus(status);
            // 如果状态是"在读"，清空备注；否则设置备注
            if ("在读".equals(status)) {
                student.setStatusRemark("");
            } else {
                student.setStatusRemark(statusRemark);
            }
            student.setUpdateTime(new java.util.Date());
            mongoTemplate.save(student);
            count++;
        }
        
        return count;
    }

    @Override
    public int batchUpdateMajorByIds(List<String> ids, String major) {
        int count = 0;
        for (String id : ids) {
            Optional<StudentModel> student = studentRepository.findById(id);
            if (student.isPresent()) {
                StudentModel existing = student.get();
                existing.setMajor(major.trim());
                existing.setUpdateTime(new java.util.Date());
                studentRepository.save(existing);
                count++;
            }
        }
        return count;
    }

    // 初始化默认学生用户
    public void createDefaultStudent() {
        // 创建第一个默认学生
        if (studentRepository.findByStudentId("10240001") == null) {
            StudentModel student1 = new StudentModel();
            student1.setStudentId("10240001");
            student1.setName("默认学生");
            student1.setGender("男");
            student1.setAge(24);
            student1.setPassword(passwordEncoder.encode(demoStudentPassword));
            student1.setEmail("default@student.edu.cn");
            student1.setPhone("13800000000");
            student1.setMajor("计算机科学与技术");
            student1.setGrade("2024级");
            student1.setClassName("研一1班");
            student1.setSupervisor("张老师");
            student1.setResearchDirection("人工智能");
            student1.setStatus("在读");
            student1.setCreateTime(new java.util.Date());
            student1.setUpdateTime(new java.util.Date());
            student1.setPoliticalStatus("中共党员");
            student1.setDormitory("南苑 3 栋 502");
            student1.setEmergencyContact("张老师 / 13800000000");
            student1.setNativePlace("湖北省");
            student1.setEducationType("全日制");
            student1.setMaritalStatus("未婚");
            student1.setNation("汉族");
            student1.setBirthDate(new java.util.Date(99,0,1)); // 1999-01-01
            student1.setWorkStatus("年级干部");
            studentRepository.save(student1);
        }
        
        // 创建第二个学生账号（用于测试）
        if (studentRepository.findByStudentId("20240001") == null) {
            StudentModel student2 = new StudentModel();
            student2.setStudentId("20240001");
            student2.setName("学生111");
            student2.setGender("女");
            student2.setAge(23);
            student2.setPassword(passwordEncoder.encode(demoStudentPassword));
            student2.setEmail("student111@edu.cn");
            student2.setPhone("13900000001");
            student2.setMajor("软件工程");
            student2.setGrade("2025级");
            student2.setClassName("研一2班");
            student2.setSupervisor("李老师");
            student2.setResearchDirection("软件工程");
            student2.setStatus("在读");
            student2.setCreateTime(new java.util.Date());
            student2.setUpdateTime(new java.util.Date());
            student2.setPoliticalStatus("共青团员");
            student2.setDormitory("北苑 2 栋 318");
            student2.setEmergencyContact("李老师 / 13900000001");
            student2.setNativePlace("湖北省");
            student2.setEducationType("全日制");
            student2.setMaritalStatus("未婚");
            student2.setNation("汉族");
            student2.setBirthDate(new java.util.Date(100,0,1)); // 2000-01-01
            student2.setWorkStatus("班级干部");
            studentRepository.save(student2);
        }
        
        // 创建测试学生用例（用户指定的默认学生）
        if (studentRepository.findByStudentId("102411111") == null) {
            StudentModel testStudent = new StudentModel();
            testStudent.setStudentId("102411111");
            testStudent.setName("测试学生用例");
            testStudent.setGender("男");
            testStudent.setAge(22);
            testStudent.setPassword(passwordEncoder.encode("Hbut_411111"));
            testStudent.setEmail("test@student.edu.cn");
            testStudent.setPhone("13800000002");
            testStudent.setMajor("计算机科学与技术");
            testStudent.setGrade("2026级");
            testStudent.setClassName("研一1班");
            testStudent.setSupervisor("测试导师");
            testStudent.setResearchDirection("软件测试");
            testStudent.setStatus("在读");
            testStudent.setCreateTime(new java.util.Date());
            testStudent.setUpdateTime(new java.util.Date());
            testStudent.setPoliticalStatus("共青团员");
            testStudent.setDormitory("东苑 5 栋 616");
            testStudent.setEmergencyContact("测试导师 / 13800000002");
            testStudent.setNativePlace("湖北省");
            testStudent.setEducationType("全日制");
            testStudent.setMaritalStatus("未婚");
            testStudent.setNation("汉族");
            testStudent.setBirthDate(new java.util.Date(102,0,1)); // 2002-01-01
            testStudent.setWorkStatus("普通学生");
            studentRepository.save(testStudent);
            System.out.println("已创建默认测试学生：学号 102411111，姓名 测试学生用例");
        }
    }

    @Override
    public boolean resetStudentPassword(String studentId, String newPassword) {
        try {
            StudentModel student = studentRepository.findByStudentId(studentId);
            if (student == null) {
                return false;
            }
            String passwordToSet = newPassword == null || newPassword.isEmpty()
                    ? PasswordPolicy.legacyPassword(studentId) : newPassword;
            PasswordPolicy.validate(passwordToSet);
            // 加密密码
            student.setPassword(passwordEncoder.encode(passwordToSet));
            student.setPasswordChangeRequired(true);
            student.setInitialPasswordExpiresAt(PasswordPolicy.legacyPassword(studentId).equals(passwordToSet)
                    ? null : new java.util.Date(System.currentTimeMillis() + 86400000L));
            student.setUpdateTime(new java.util.Date());
            studentRepository.save(student);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean changePassword(String studentId, String oldPassword, String newPassword) {
        try {
            PasswordPolicy.validate(newPassword);
            if (PasswordPolicy.legacyPassword(studentId).equals(newPassword)) return false;
            // 验证新密码规则：不少于8位，必须包含数字和字母
            if (newPassword == null || newPassword.length() < 8) {
                throw new RuntimeException("密码长度不能少于8位");
            }
            if (!newPassword.matches(".*\\d.*")) {
                throw new RuntimeException("密码必须包含至少一个数字");
            }
            if (!newPassword.matches(".*[a-zA-Z].*")) {
                throw new RuntimeException("密码必须包含至少一个字母");
            }
            
            StudentModel student = studentRepository.findByStudentId(studentId);
            if (student == null) {
                return false;
            }
            if (student.getPassword() == null || student.getPassword().isEmpty()
                    || oldPassword == null || oldPassword.isEmpty()
                    || !passwordEncoder.matches(oldPassword, student.getPassword())) return false;
            if (Boolean.TRUE.equals(student.getPasswordChangeRequired()) && !isDefaultPassword(studentId, oldPassword)) return false;
            if (passwordEncoder.matches(newPassword, student.getPassword()))
                throw new IllegalArgumentException("新密码不能与当前密码相同");
            student.setPassword(passwordEncoder.encode(newPassword));
            student.setPasswordChangeRequired(false);
            student.setInitialPasswordExpiresAt(null);
            student.setUpdateTime(new java.util.Date());
            studentRepository.save(student);
            return true;
        } catch (RuntimeException e) {
            // 重新抛出业务异常，让Controller层处理
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isDefaultPassword(String studentId, String password) {
        try {
            StudentModel student = studentRepository.findByStudentId(studentId);
            if (student == null || student.getPassword() == null || student.getPassword().isEmpty()
                    || password == null || password.isEmpty()) {
                return false;
            }
            return (Boolean.TRUE.equals(student.getPasswordChangeRequired())
                        || PasswordPolicy.legacyPassword(studentId).equals(password))
                    && (student.getInitialPasswordExpiresAt() == null
                        || student.getInitialPasswordExpiresAt().after(new java.util.Date()))
                    && passwordEncoder.matches(password, student.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
