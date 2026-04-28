package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.StudentModel;
import org.example.service.*;
import org.example.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/msi/student")
public class StudentController {
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private org.example.service.OnlineUserService onlineUserService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String account = loginForm.get("account");
        String password = loginForm.get("password");
        
        Map<String, Object> result = new HashMap<>();
        StudentModel student = studentService.login(account, password);
        
        if (student != null) {
            // 检查密码是否为初始密码
            boolean isDefaultPassword = studentService.isDefaultPassword(student.getStudentId(), password);
            
            if (isDefaultPassword) {
                // 密码是初始密码，需要强制修改
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "检测到您使用的是初始密码，请先修改密码后再登录");
                result.put("requirePasswordChange", true);
                result.put("studentId", student.getStudentId());
                result.put("studentName", student.getName());
                return ResponseEntity.ok(result);
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(student.getId(), "student", student.getName());
            
            // 记录用户在线
            try {
                onlineUserService.recordUserOnline(student.getId(), "student");
            } catch (Exception e) {
                // Redis连接失败不影响登录
            }
            
            // 返回token和基本用户信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", student.getId());
            data.put("studentId", student.getStudentId());
            data.put("name", student.getName());
            data.put("userType", "student");
            
            result.put("code", 200);
            result.put("data", data);
            result.put("msg", "登录成功");
        } else {
            result.put("code", 401);
            result.put("data", null);
            result.put("msg", "账号或密码错误");
        }
        return ResponseEntity.ok(result);
    }

    // 获取所有学生（管理员权限）
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllStudents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String grade) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = studentService.findStudentsPage(page, size, name, studentId, major, grade);
            result.put("code", 200);
            result.put("data", data);
            result.put("msg", "获取学生列表成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取学生列表失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 根据ID获取学生信息
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            StudentModel student = studentService.findById(id);
            if (student != null) {
                result.put("code", 200);
                result.put("data", student);
                result.put("msg", "获取学生信息成功");
            } else {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "学生不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取学生信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 创建学生（管理员权限）
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody StudentModel student) {
        Map<String, Object> result = new HashMap<>();
        try {
            StudentModel createdStudent = studentService.createStudent(student);
            result.put("code", 200);
            result.put("data", createdStudent);
            result.put("msg", "学生创建成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "创建失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 更新学生信息
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateStudent(@RequestBody StudentModel student) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查ID是否存在
            if (student.getId() == null || student.getId().isEmpty()) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "学生ID不能为空");
                return ResponseEntity.ok(result);
            }
            
            StudentModel updatedStudent = studentService.updateStudent(student);
            if (updatedStudent == null) {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "学生不存在");
            } else {
                result.put("code", 200);
                result.put("data", updatedStudent);
                result.put("msg", "学生信息更新成功");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "更新失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 删除学生（管理员权限）
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = studentService.deleteStudent(id);
            if (success) {
                result.put("code", 200);
                result.put("msg", "学生删除成功");
            } else {
                result.put("code", 404);
                result.put("msg", "学生不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "删除失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取学生统计信息
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalStudents", studentService.findAllStudents().size());
            statistics.put("activeStudents", studentService.findAllStudents().stream()
                .filter(s -> "在读".equals(s.getStatus())).count());
            statistics.put("graduatedStudents", studentService.findAllStudents().stream()
                .filter(s -> "毕业".equals(s.getStatus())).count());
            statistics.put("maleStudents", studentService.findAllStudents().stream()
                .filter(s -> "男".equals(s.getGender())).count());
            statistics.put("femaleStudents", studentService.findAllStudents().stream()
                .filter(s -> "女".equals(s.getGender())).count());
            
            result.put("code", 200);
            result.put("data", statistics);
            result.put("msg", "获取统计信息成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取统计信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取最新动态
    @GetMapping("/news")
    public ResponseEntity<Map<String, Object>> getLatestNews() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 模拟最新动态数据
            Map<String, Object> news = new HashMap<>();
            news.put("title", "系统公告");
            news.put("content", "欢迎使用学生管理系统！");
            news.put("time", new java.util.Date());
            
            result.put("code", 200);
            result.put("data", news);
            result.put("msg", "获取最新动态成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取最新动态失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取图表数据
    @GetMapping("/chart")
    public ResponseEntity<Map<String, Object>> getChartData() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 模拟图表数据
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", new String[]{"计算机科学与技术", "软件工程", "信息安全"});
            chartData.put("data", new int[]{120, 80, 60});
            
            result.put("code", 200);
            result.put("data", chartData);
            result.put("msg", "获取图表数据成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取图表数据失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @Autowired
    private CompetitionService competitionService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private PaperService paperService;
    @Autowired
    private InternshipEmploymentService internshipEmploymentService;
    @Autowired
    private AcademicEventService academicEventService;
    @Autowired
    private DailyActivityService dailyActivityService;
    @Autowired
    private HonorService honorService;
    @Autowired
    private DailyTaskService dailyTaskService;
    @Autowired
    private org.example.repository.DailyTaskSubmissionRepository dailyTaskSubmissionRepository;

    // 获取学生个人统计数据
    @GetMapping("/personal-stats/{studentId}")
    public ResponseEntity<Map<String, Object>> getPersonalStats(@PathVariable String studentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从数据库获取真实的个人统计数据
            Map<String, Object> personalStats = new HashMap<>();
            
            // 注意：数据库中存储的studentId字段是学号，不是数据库ID
            // 所以直接使用学号查询即可
            personalStats.put("internshipCount", internshipEmploymentService.getByStudentId(studentId).size());
            personalStats.put("competitionCount", competitionService.getStudentCompetitions(studentId).size());
            personalStats.put("paperCount", paperService.getStudentPapers(studentId).size());
            personalStats.put("projectCount", projectService.getStudentProjects(studentId).size());
            
            result.put("code", 200);
            result.put("data", personalStats);
            result.put("msg", "获取个人统计数据成功");
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常堆栈，便于调试
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取个人统计数据失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取当前用户个人信息
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从JWT Token中获取当前用户ID
            String userId = (String) currentUser.get("userId");
            StudentModel student = studentService.findById(userId);
            if (student != null) {
                result.put("code", 200);
                result.put("data", student);
                result.put("msg", "获取个人信息成功");
            } else {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "用户信息不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取个人信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取当前用户基本信息（用于页面刷新后恢复用户信息）
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            result.put("code", 401);
            result.put("data", null);
            result.put("msg", "缺少有效的Authorization头");
            return ResponseEntity.ok(result);
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtUtil.validateToken(token)) {
            result.put("code", 401);
            result.put("data", null);
            result.put("msg", "Token无效或已过期");
            return ResponseEntity.ok(result);
        }
        
        try {
            String userId = jwtUtil.getUserIdFromToken(token);
            String userType = jwtUtil.getUserTypeFromToken(token);
            
            if ("student".equals(userType)) {
                StudentModel student = studentService.findById(userId);
                if (student != null) {
                    // 只返回基本信息，不包含敏感信息
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", student.getId());
                    userInfo.put("studentId", student.getStudentId());
                    userInfo.put("name", student.getName());
                    userInfo.put("userType", "student");
                    
                    result.put("code", 200);
                    result.put("data", userInfo);
                    result.put("msg", "获取当前用户信息成功");
                } else {
                    result.put("code", 404);
                    result.put("data", null);
                    result.put("msg", "用户信息不存在");
                }
            } else {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "用户类型不匹配");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取当前用户信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // Token刷新接口
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            result.put("code", 401);
            result.put("data", null);
            result.put("msg", "缺少Authorization头");
            return ResponseEntity.ok(result);
        }
        
        String token = authHeader.substring(7);
        
        try {
            // 即使token即将过期，也允许刷新
            if (jwtUtil.validateToken(token)) {
                // 检查token是否即将过期
                if (jwtUtil.isTokenNearExpiration(token)) {
                    // 生成新的token
                    String newToken = jwtUtil.refreshToken(token);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", newToken);
                    data.put("userId", jwtUtil.getUserIdFromToken(newToken));
                    data.put("userType", jwtUtil.getUserTypeFromToken(newToken));
                    data.put("username", jwtUtil.getUsernameFromToken(newToken));
                    
                    result.put("code", 200);
                    result.put("data", data);
                    result.put("msg", "Token刷新成功");
                } else {
                    result.put("code", 200);
                    result.put("data", null);
                    result.put("msg", "Token尚未过期，无需刷新");
                }
            } else {
                result.put("code", 401);
                result.put("data", null);
                result.put("msg", "Token无效或已过期");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Token刷新失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    // 更新个人信息
    @PutMapping("/profile/update")
    public ResponseEntity<Map<String, Object>> updateUserProfile(@RequestBody StudentModel student, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 确保只能更新自己的信息
            String userId = (String) currentUser.get("userId");
            student.setId(userId);
            
            StudentModel updatedStudent = studentService.updateStudent(student);
            result.put("code", 200);
            result.put("data", updatedStudent);
            result.put("msg", "更新个人信息成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "更新个人信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @Autowired
    private org.example.repository.StudyRecordRepository studyRecordRepository;

    // 批量导入学生
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "grade", required = false) String grade) {
        Map<String, Object> result = new HashMap<>();
        try {
            studentService.importStudents(file.getInputStream(), grade);
            result.put("code", 200);
            result.put("msg", "导入成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "导入失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取所有年级列表
    @GetMapping("/grades")
    public ResponseEntity<Map<String, Object>> getAllGrades() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> grades = studentService.getAllGrades();
            result.put("code", 200);
            result.put("data", grades);
            result.put("msg", "获取年级列表成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取年级列表失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 批量修改学生状态（按年级）
    @PutMapping("/batch-update-status")
    public ResponseEntity<Map<String, Object>> batchUpdateStatus(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String grade = (String) requestBody.get("grade");
            String status = (String) requestBody.get("status");
            String statusRemark = (String) requestBody.get("statusRemark");
            
            if (grade == null || grade.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "年级不能为空");
                return ResponseEntity.ok(result);
            }
            
            if (status == null || status.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "状态不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 如果状态不是"在读"，必须填写备注
            if (!"在读".equals(status) && (statusRemark == null || statusRemark.trim().isEmpty())) {
                result.put("code", 400);
                result.put("msg", "非在读状态必须填写备注信息");
                return ResponseEntity.ok(result);
            }
            
            int updatedCount = studentService.batchUpdateStatusByGrade(grade, status, statusRemark);
            result.put("code", 200);
            result.put("data", updatedCount);
            result.put("msg", "成功修改 " + updatedCount + " 个学生的状态");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "批量修改状态失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 批量删除学生
    @DeleteMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDeleteStudents(@RequestBody Map<String, List<String>> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> ids = requestBody.get("ids");
            if (ids == null || ids.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "请选择要删除的学生");
                return ResponseEntity.ok(result);
            }
            
            int deletedCount = 0;
            for (String id : ids) {
                if (studentService.deleteStudent(id)) {
                    deletedCount++;
                }
            }
            
            result.put("code", 200);
            result.put("msg", "成功删除 " + deletedCount + " 个学生");
            result.put("data", deletedCount);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "批量删除失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取学习记录
    @GetMapping("/study-records")
    public ResponseEntity<Map<String, Object>> getStudyRecords(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从JWT Token中获取当前用户ID
            String userId = (String) currentUser.get("userId");
            
            // 从数据库获取真实的学习记录
            List<org.example.model.StudyRecordModel> studyRecords = studyRecordRepository.findByStudentDbId(userId);
            
            result.put("code", 200);
            result.put("data", studyRecords);
            result.put("msg", "获取学习记录成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取学习记录失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 管理员重置学生密码（重置为初始密码：Hbut_学号后六位）
    @PutMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String studentId = requestBody.get("studentId");
            
            if (studentId == null || studentId.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "学号不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 重置为初始密码（Hbut_学号后六位），传入null表示使用默认密码
            try {
                boolean success = studentService.resetStudentPassword(studentId, null);
                if (success) {
                    // 计算初始密码用于提示
                    String lastSix = studentId.length() > 6 ? studentId.substring(studentId.length() - 6) : studentId;
                    String defaultPassword = "Hbut_" + lastSix;
                    result.put("code", 200);
                    result.put("msg", "密码已重置为初始密码：" + defaultPassword);
                    result.put("data", defaultPassword); // 返回初始密码，方便管理员告知学生
                } else {
                    result.put("code", 404);
                    result.put("msg", "学生不存在");
                }
            } catch (RuntimeException e) {
                // 捕获密码规则验证异常（如果管理员手动输入新密码时）
                result.put("code", 400);
                result.put("msg", e.getMessage());
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "密码重置失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 修改初始密码（不需要登录，用于首次登录时强制修改密码）
    @PutMapping("/change-initial-password")
    public ResponseEntity<Map<String, Object>> changeInitialPassword(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String studentId = requestBody.get("studentId");
            String newPassword = requestBody.get("newPassword");
            
            if (studentId == null || studentId.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "学号不能为空");
                return ResponseEntity.ok(result);
            }
            
            if (newPassword == null || newPassword.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "新密码不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 验证学号是否存在
            StudentModel student = studentService.findByStudentId(studentId);
            if (student == null) {
                result.put("code", 404);
                result.put("msg", "学生不存在");
                return ResponseEntity.ok(result);
            }
            
            // 验证当前密码是否为初始密码（通过检查数据库中的密码是否为初始密码的加密值）
            boolean isDefaultPassword = studentService.isDefaultPassword(studentId, "");
            if (!isDefaultPassword) {
                result.put("code", 400);
                result.put("msg", "当前密码不是初始密码，无法使用此接口修改");
                return ResponseEntity.ok(result);
            }
            
            // 修改密码（传入空字符串作为旧密码，因为初始密码修改不需要验证旧密码）
            try {
                boolean success = studentService.changePassword(studentId, "", newPassword);
                if (success) {
                    result.put("code", 200);
                    result.put("msg", "密码修改成功，请重新登录");
                } else {
                    result.put("code", 500);
                    result.put("msg", "密码修改失败，请检查密码是否符合要求");
                }
            } catch (RuntimeException e) {
                // 捕获密码规则验证异常
                result.put("code", 400);
                result.put("msg", e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常堆栈，便于调试
            result.put("code", 500);
            result.put("msg", "密码修改失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 学生修改自己的密码
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> requestBody, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String oldPassword = requestBody.get("oldPassword");
            String newPassword = requestBody.get("newPassword");
            
            if (newPassword == null || newPassword.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "新密码不能为空");
                return ResponseEntity.ok(result);
            }
            
            // 从JWT Token中获取当前用户ID
            String userId = (String) currentUser.get("userId");
            StudentModel student = studentService.findById(userId);
            if (student == null) {
                result.put("code", 404);
                result.put("msg", "用户不存在");
                return ResponseEntity.ok(result);
            }
            
            String studentId = student.getStudentId();
            
            // 如果用户有密码，需要验证旧密码
            if (student.getPassword() != null && !student.getPassword().isEmpty()) {
                if (oldPassword == null || oldPassword.isEmpty()) {
                    result.put("code", 400);
                    result.put("msg", "请输入旧密码");
                    return ResponseEntity.ok(result);
                }
            }
            
            try {
                boolean success = studentService.changePassword(studentId, oldPassword, newPassword);
                if (success) {
                    result.put("code", 200);
                    result.put("msg", "密码修改成功");
                } else {
                    result.put("code", 400);
                    result.put("msg", "旧密码错误");
                }
            } catch (RuntimeException e) {
                // 捕获密码规则验证异常
                result.put("code", 400);
                result.put("msg", e.getMessage());
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "密码修改失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取最近活动
    @GetMapping("/recent-activities")
    public ResponseEntity<Map<String, Object>> getRecentActivities(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String userId = (String) currentUser.get("userId");
            List<Map<String, Object>> activities = new java.util.ArrayList<>();
            
            // 1. 获取学术活动
            List<org.example.model.AcademicEventModel> academicEvents = academicEventService.findByStudentId(userId);
            for (org.example.model.AcademicEventModel event : academicEvents) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", "academic_" + event.getId());
                activity.put("type", "学术活动");
                activity.put("title", event.getTitle());
                activity.put("description", "参加了" + event.getTitle() + "，主办方：" + (event.getOrganizer() != null ? event.getOrganizer() : "未知"));
                activity.put("time", event.getCreateTime() != null ? event.getCreateTime().toString() : 
                    (event.getStartDate() != null ? event.getStartDate().toString() : "未知时间"));
                activities.add(activity);
            }
            
            // 2. 获取日常活动
            List<org.example.model.DailyActivityModel> dailyActivities = dailyActivityService.findByStudentId(userId);
            for (org.example.model.DailyActivityModel activity : dailyActivities) {
                Map<String, Object> act = new HashMap<>();
                act.put("id", "daily_" + activity.getId());
                act.put("type", "日常活动");
                act.put("title", activity.getTitle());
                act.put("description", "参与了" + activity.getTitle() + "，级别：" + (activity.getLevel() != null ? activity.getLevel() : "未知"));
                act.put("time", activity.getCreateTime() != null ? activity.getCreateTime().toString() : 
                    (activity.getStartDate() != null ? activity.getStartDate().toString() : "未知时间"));
                activities.add(act);
            }
            
            // 3. 获取荣誉
            List<org.example.model.HonorModel> honors = honorService.findByUserId(userId);
            for (org.example.model.HonorModel honor : honors) {
                Map<String, Object> act = new HashMap<>();
                act.put("id", "honor_" + honor.getId());
                act.put("type", "荣誉");
                act.put("title", honor.getTitle());
                act.put("description", "获得" + honor.getTitle() + "，授予单位：" + (honor.getAwardOrg() != null ? honor.getAwardOrg() : "未知"));
                act.put("time", honor.getCreateTime() != null ? honor.getCreateTime().toString() : 
                    (honor.getAwardDate() != null ? honor.getAwardDate().toString() : "未知时间"));
                activities.add(act);
            }
            
            // 4. 获取任务提交
            List<org.example.model.DailyTaskSubmissionModel> taskSubmissions = dailyTaskSubmissionRepository.findByStudentId(userId);
            for (org.example.model.DailyTaskSubmissionModel submission : taskSubmissions) {
                org.example.model.DailyTaskModel task = dailyTaskService.getTaskById(submission.getTaskId());
                if (task != null) {
                    Map<String, Object> act = new HashMap<>();
                    act.put("id", "task_" + submission.getId());
                    act.put("type", "任务完成");
                    act.put("title", "完成任务：" + task.getTitle());
                    act.put("description", "完成了任务：" + task.getTitle());
                    act.put("time", submission.getSubmissionTime() != null ? submission.getSubmissionTime().toString() : 
                        (submission.getUpdateTime() != null ? submission.getUpdateTime().toString() : "未知时间"));
                    activities.add(act);
                }
            }
            
            // 5. 获取实习就业
            List<org.example.model.InternshipEmploymentModel> internships = internshipEmploymentService.getByUserId(userId);
            for (org.example.model.InternshipEmploymentModel internship : internships) {
                Map<String, Object> act = new HashMap<>();
                act.put("id", "internship_" + internship.getId());
                act.put("type", "实习就业");
                act.put("title", internship.getCompany() != null ? "在" + internship.getCompany() + "实习" : "实习申请");
                act.put("description", internship.getPosition() != null ? "申请了" + internship.getPosition() + "职位" : "提交了实习申请");
                act.put("time", internship.getCreateTime() != null ? internship.getCreateTime().toString() : "未知时间");
                activities.add(act);
            }
            
            // 6. 获取竞赛
            List<org.example.model.CompetitionModel> competitions = competitionService.getStudentCompetitions(userId);
            for (org.example.model.CompetitionModel competition : competitions) {
                Map<String, Object> act = new HashMap<>();
                act.put("id", "competition_" + competition.getId());
                act.put("type", "竞赛");
                act.put("title", competition.getCompetitionName() != null ? competition.getCompetitionName() : "竞赛");
                act.put("description", competition.getAwardLevel() != null ? "参加了" + competition.getCompetitionName() + "，获得" + competition.getAwardLevel() : 
                    "参加了" + competition.getCompetitionName());
                act.put("time", competition.getCreateTime() != null ? competition.getCreateTime().toString() : 
                    (competition.getAwardDate() != null ? competition.getAwardDate().toString() : "未知时间"));
                activities.add(act);
            }
            
            // 7. 获取论文
            List<org.example.model.PaperModel> papers = paperService.getStudentPapers(userId);
            for (org.example.model.PaperModel paper : papers) {
                Map<String, Object> act = new HashMap<>();
                act.put("id", "paper_" + paper.getId());
                act.put("type", "论文");
                act.put("title", paper.getPaperTitle() != null ? paper.getPaperTitle() : "论文");
                act.put("description", "发表了论文：" + (paper.getPaperTitle() != null ? paper.getPaperTitle() : "未知标题"));
                act.put("time", paper.getCreateTime() != null ? paper.getCreateTime().toString() : 
                    (paper.getPublicationDate() != null ? paper.getPublicationDate().toString() : "未知时间"));
                activities.add(act);
            }
            
            // 8. 获取项目
            List<org.example.model.ProjectModel> projects = projectService.getStudentProjects(userId);
            for (org.example.model.ProjectModel project : projects) {
                Map<String, Object> act = new HashMap<>();
                act.put("id", "project_" + project.getId());
                act.put("type", "项目");
                act.put("title", project.getProjectName() != null ? project.getProjectName() : "项目");
                act.put("description", "参与了项目：" + (project.getProjectName() != null ? project.getProjectName() : "未知项目"));
                act.put("time", project.getCreateTime() != null ? project.getCreateTime().toString() : 
                    (project.getStartDate() != null ? project.getStartDate().toString() : "未知时间"));
                activities.add(act);
            }
            
            // 按时间排序（最新的在前），取前5条（首页显示）
            activities.sort((a, b) -> {
                String timeA = (String) a.get("time");
                String timeB = (String) b.get("time");
                if (timeA == null || timeA.equals("未知时间")) return 1;
                if (timeB == null || timeB.equals("未知时间")) return -1;
                return timeB.compareTo(timeA); // 降序
            });
            
            if (activities.size() > 5) {
                activities = activities.subList(0, 5);
            }
            
            result.put("code", 200);
            result.put("data", activities);
            result.put("msg", "获取最近活动成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取最近活动失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
