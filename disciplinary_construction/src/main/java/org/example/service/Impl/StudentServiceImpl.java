package org.example.service.Impl;


import org.example.model.StudentModel;
import org.example.model.DailyTaskModel;
import org.example.model.DailyTaskSubmissionModel;
import org.example.repository.StudentRepository;
import org.example.repository.DailyTaskRepository;
import org.example.repository.DailyTaskSubmissionRepository;
import org.example.service.StudentService;
import org.example.util.StudentGradePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        // 自动修复：如果数据库中密码为空，为其设置初始密码
        if (student.getPassword() == null || student.getPassword().isEmpty()) {
            String sid = student.getStudentId();
            String lastSix = sid.length() > 6 ? sid.substring(sid.length() - 6) : sid;
            String defaultRawPassword = "Hbut_" + lastSix;
            student.setPassword(passwordEncoder.encode(defaultRawPassword));
            studentRepository.save(student); // 保存回数据库
        }

        if (passwordEncoder.matches(password, student.getPassword())) {
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
        student.setGrade(StudentGradePolicy.requireValid(student.getGrade()));
        student.setCreateTime(new java.util.Date());
        student.setUpdateTime(new java.util.Date());
        // 加密密码
        if (student.getPassword() != null && !student.getPassword().startsWith("$2a$")) {
            student.setPassword(passwordEncoder.encode(student.getPassword()));
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
            if (!student.getPassword().startsWith("$2a$")) {
                existing.setPassword(passwordEncoder.encode(student.getPassword()));
            } else {
                existing.setPassword(student.getPassword());
            }
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
    public void importStudents(java.io.InputStream inputStream, String grade) throws Exception {
        final String selectedGrade = grade == null || grade.trim().isEmpty()
                ? null
                : StudentGradePolicy.requireValid(grade);
        cn.hutool.poi.excel.ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(inputStream);
        
        // 设置表头映射，只保留核心必填字段
        reader.addHeaderAlias("学号", "studentId");
        reader.addHeaderAlias("姓名", "name");
        reader.addHeaderAlias("专业", "major");
        reader.addHeaderAlias("年级", "grade");
        reader.addHeaderAlias("班级", "className");

        List<StudentModel> students = reader.readAll(StudentModel.class);
        
        for (StudentModel student : students) {
            // 姓名和学号是必填项，如果缺失则跳过
            if (student.getStudentId() == null || student.getStudentId().isEmpty() || 
                student.getName() == null || student.getName().isEmpty()) {
                continue;
            }

            // 检查学号是否已存在
            if (studentRepository.findByStudentId(student.getStudentId()) != null) {
                continue; // 保持现有记录，不覆盖
            }
            
            // 设置初始信息
            student.setCreateTime(new java.util.Date());
            student.setUpdateTime(new java.util.Date());
            
            // 设置默认状态
            if (student.getStatus() == null || student.getStatus().isEmpty()) {
                student.setStatus("在读");
            }
            
            // 如果指定了年级，无论Excel中是否有年级信息，都设置为指定的年级
            student.setGrade(selectedGrade != null
                    ? selectedGrade
                    : StudentGradePolicy.requireValid(student.getGrade()));
            
            // 初始密码为 Hbut_（学号后六位）
            String sid = student.getStudentId();
            String lastSix = sid.length() > 6 ? sid.substring(sid.length() - 6) : sid;
            String rawPassword = "Hbut_" + lastSix;
            student.setPassword(passwordEncoder.encode(rawPassword));
            
            studentRepository.save(student);
        }
        reader.close();
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
            // 如果newPassword为空，则使用初始密码格式：Hbut_学号后六位
            String passwordToSet;
            if (newPassword == null || newPassword.isEmpty()) {
                String lastSix = studentId.length() > 6 ? studentId.substring(studentId.length() - 6) : studentId;
                passwordToSet = "Hbut_" + lastSix;
            } else {
                passwordToSet = newPassword;
            }
            // 加密密码
            student.setPassword(passwordEncoder.encode(passwordToSet));
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
            // 验证旧密码
            if (student.getPassword() == null || student.getPassword().isEmpty()) {
                // 如果密码为空，允许直接设置新密码
                student.setPassword(passwordEncoder.encode(newPassword));
            } else if (oldPassword == null || oldPassword.isEmpty()) {
                // 如果旧密码为空，检查是否为初始密码修改场景
                // 验证当前密码是否为初始密码
                String lastSix = studentId.length() > 6 ? studentId.substring(studentId.length() - 6) : studentId;
                String defaultPassword = "Hbut_" + lastSix;
                if (passwordEncoder.matches(defaultPassword, student.getPassword())) {
                    // 是初始密码，允许直接修改
                    student.setPassword(passwordEncoder.encode(newPassword));
                } else {
                    // 不是初始密码，不允许修改
                    return false;
                }
            } else {
                // 验证旧密码是否正确
                if (!passwordEncoder.matches(oldPassword, student.getPassword())) {
                    return false; // 旧密码错误
                }
                // 设置新密码
                student.setPassword(passwordEncoder.encode(newPassword));
            }
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
            // 计算初始密码
            String lastSix = studentId.length() > 6 ? studentId.substring(studentId.length() - 6) : studentId;
            String defaultPassword = "Hbut_" + lastSix;
            // 验证数据库中存储的密码是否为初始密码（通过匹配初始密码的加密值）
            return defaultPassword.equals(password)
                    && passwordEncoder.matches(password, student.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
