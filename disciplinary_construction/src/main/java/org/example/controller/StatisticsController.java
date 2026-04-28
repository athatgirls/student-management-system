package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/msi/admin/audit")
public class StatisticsController {

    @Autowired
    private CompetitionService competitionService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private PaperService paperService;
    @Autowired
    private PatentService patentService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private InternshipEmploymentService internshipEmploymentService;
    @Autowired
    private AlumniService alumniService;
    @Autowired
    private AcademicEventService academicEventService;
    @Autowired
    private DailyActivityService dailyActivityService;
    @Autowired
    private HonorService honorService;
    @Autowired
    private ThoughtReportService thoughtReportService;
    @Autowired
    private PartyCourseService partyCourseService;
    @Autowired
    private VolunteerServiceService volunteerServiceService;
    @Autowired
    private AcademicEventRepository academicEventRepository;
    @Autowired
    private DailyActivityRepository dailyActivityRepository;
    @Autowired
    private HonorRepository honorRepository;
    @Autowired
    private PartyApplicationRepository partyApplicationRepository;
    @Autowired
    private DailyTaskSubmissionRepository dailyTaskSubmissionRepository;
    @Autowired
    private DailyTaskRepository dailyTaskRepository;
    @Autowired
    private org.example.service.OnlineUserService onlineUserService;
    @Autowired
    private org.example.service.VisitStatisticsService visitStatisticsService;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            // 基础数量统计
            Map<String, Object> counts = new HashMap<>();
            List<StudentModel> allStudents = studentService.findAllStudents();
            counts.put("totalStudents", allStudents.size());
            counts.put("internshipCount", internshipEmploymentService.getAll().size());
            counts.put("competitionCount", competitionService.getAllCompetitions().size());
            // 管理员查看，isAdmin=true，显示所有校友信息
            counts.put("alumniCount", alumniService.getTotalCount("", null, true));
            result.put("statistics", counts);

            // 专业分布统计 (用于图表)
            Map<String, Long> majorDistribution = allStudents.stream()
                    .filter(s -> s.getMajor() != null && !s.getMajor().trim().isEmpty())
                    .collect(Collectors.groupingBy(StudentModel::getMajor, Collectors.counting()));
            
            List<Map<String, Object>> chartData = majorDistribution.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", entry.getKey());
                        item.put("value", entry.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());
            
            // 如果没有专业数据，返回空数组
            result.put("majorChartData", chartData);

            // 最近动态 (聚合多个模块的最新记录)
            List<Map<String, Object>> activities = new java.util.ArrayList<>();
            
            // 1. 最新完善个人信息的学生（从任务提交记录中获取）
            try {
                // 查找"完善个人信息"任务
                Optional<DailyTaskModel> profileTask = dailyTaskRepository.findByTitle("【系统】请完善个人基本信息");
                if (profileTask.isPresent()) {
                    String taskId = profileTask.get().getId();
                    // 查询该任务的所有提交记录
                    List<DailyTaskSubmissionModel> submissions = dailyTaskSubmissionRepository.findByTaskId(taskId);
                    if (!submissions.isEmpty()) {
                        // 找到最新的提交记录
                        DailyTaskSubmissionModel latestSubmission = submissions.stream()
                            .filter(s -> s.getSubmissionTime() != null)
                            .max((s1, s2) -> s1.getSubmissionTime().compareTo(s2.getSubmissionTime()))
                            .orElse(null);
                        if (latestSubmission != null) {
                            Map<String, Object> activity = new HashMap<>();
                            activity.put("id", "profile_" + latestSubmission.getId());
                            activity.put("title", "完善个人信息");
                            activity.put("description", latestSubmission.getStudentName() + " 同学已完善个人基本信息");
                            activity.put("time", latestSubmission.getSubmissionTime().format(formatter));
                            activities.add(activity);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取最新完善个人信息记录失败", e);
            }
            
            // 2. 最新学生注册（作为补充信息）
            if (!allStudents.isEmpty()) {
                StudentModel latestStudent = allStudents.stream()
                    .filter(s -> s.getCreateTime() != null)
                    .max((s1, s2) -> s1.getCreateTime().compareTo(s2.getCreateTime()))
                    .orElse(null);
                if (latestStudent != null) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "student_" + latestStudent.getId());
                    activity.put("title", "新学生入库");
                    activity.put("description", latestStudent.getName() + " 同学的基本信息已录入系统");
                    // 处理Date类型的时间
                    if (latestStudent.getCreateTime() != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        activity.put("time", sdf.format(latestStudent.getCreateTime()));
                    } else {
                        activity.put("time", LocalDateTime.now().format(formatter));
                    }
                    activities.add(activity);
                }
            }
            
            // 3. 最新学术活动
            try {
                List<AcademicEventModel> academicEvents = academicEventService.findAll();
                if (!academicEvents.isEmpty()) {
                    AcademicEventModel latest = academicEvents.stream()
                        .filter(e -> e.getCreateTime() != null)
                        .max((e1, e2) -> e1.getCreateTime().compareTo(e2.getCreateTime()))
                        .orElse(null);
                    if (latest != null) {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("id", "academic_" + latest.getId());
                        activity.put("title", "新增学术活动");
                        activity.put("description", latest.getTitle() != null ? latest.getTitle() : "新的学术活动记录");
                        activity.put("time", latest.getCreateTime().format(formatter));
                        activities.add(activity);
                    }
                }
            } catch (Exception e) {
                log.warn("获取学术活动失败", e);
            }
            
            // 4. 最新日常活动
            try {
                List<DailyActivityModel> dailyActivities = dailyActivityService.findAll();
                if (!dailyActivities.isEmpty()) {
                    DailyActivityModel latest = dailyActivities.stream()
                        .filter(e -> e.getCreateTime() != null)
                        .max((e1, e2) -> e1.getCreateTime().compareTo(e2.getCreateTime()))
                        .orElse(null);
                    if (latest != null) {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("id", "daily_" + latest.getId());
                        activity.put("title", "新增日常活动");
                        activity.put("description", latest.getTitle() != null ? latest.getTitle() : "新的日常活动记录");
                        activity.put("time", latest.getCreateTime().format(formatter));
                        activities.add(activity);
                    }
                }
            } catch (Exception e) {
                log.warn("获取日常活动失败", e);
            }
            
            // 5. 最新荣誉记录
            try {
                List<HonorModel> honors = honorService.findAll();
                if (!honors.isEmpty()) {
                    HonorModel latest = honors.stream()
                        .filter(e -> e.getCreateTime() != null)
                        .max((e1, e2) -> e1.getCreateTime().compareTo(e2.getCreateTime()))
                        .orElse(null);
                    if (latest != null) {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("id", "honor_" + latest.getId());
                        activity.put("title", "新增荣誉记录");
                        activity.put("description", latest.getTitle() != null ? latest.getTitle() : "新的荣誉记录");
                        activity.put("time", latest.getCreateTime().format(formatter));
                        activities.add(activity);
                    }
                }
            } catch (Exception e) {
                log.warn("获取荣誉记录失败", e);
            }
            
            // 6. 最新竞赛记录
            try {
                List<?> competitions = competitionService.getAllCompetitions();
                if (!competitions.isEmpty()) {
                    Object latest = competitions.stream()
                        .filter(c -> {
                            try {
                                java.lang.reflect.Method method = c.getClass().getMethod("getCreateTime");
                                return method.invoke(c) != null;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .max((c1, c2) -> {
                            try {
                                java.lang.reflect.Method method = c1.getClass().getMethod("getCreateTime");
                                LocalDateTime time1 = (LocalDateTime) method.invoke(c1);
                                LocalDateTime time2 = (LocalDateTime) method.invoke(c2);
                                return time1.compareTo(time2);
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .orElse(null);
                    if (latest != null) {
                        try {
                            Map<String, Object> activity = new HashMap<>();
                            activity.put("id", "competition_" + System.currentTimeMillis());
                            activity.put("title", "新增竞赛记录");
                            java.lang.reflect.Method getTitle = latest.getClass().getMethod("getCompetitionName");
                            String title = (String) getTitle.invoke(latest);
                            activity.put("description", title != null ? title : "新的竞赛记录");
                            java.lang.reflect.Method getCreateTime = latest.getClass().getMethod("getCreateTime");
                            LocalDateTime createTime = (LocalDateTime) getCreateTime.invoke(latest);
                            activity.put("time", createTime != null ? createTime.format(formatter) : LocalDateTime.now().format(formatter));
                            activities.add(activity);
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取竞赛记录失败", e);
            }
            
            // 7. 最新实习记录
            try {
                List<?> internships = internshipEmploymentService.getAll();
                if (!internships.isEmpty()) {
                    Object latest = internships.stream()
                        .filter(i -> {
                            try {
                                java.lang.reflect.Method method = i.getClass().getMethod("getCreateTime");
                                return method.invoke(i) != null;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .max((i1, i2) -> {
                            try {
                                java.lang.reflect.Method method = i1.getClass().getMethod("getCreateTime");
                                java.util.Date time1 = (java.util.Date) method.invoke(i1);
                                java.util.Date time2 = (java.util.Date) method.invoke(i2);
                                return time1.compareTo(time2);
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .orElse(null);
                    if (latest != null) {
                        try {
                            Map<String, Object> activity = new HashMap<>();
                            activity.put("id", "internship_" + System.currentTimeMillis());
                            activity.put("title", "新增实习记录");
                            java.lang.reflect.Method getCompany = latest.getClass().getMethod("getCompany");
                            String company = (String) getCompany.invoke(latest);
                            activity.put("description", company != null ? company : "新的实习就业记录");
                            java.lang.reflect.Method getCreateTime = latest.getClass().getMethod("getCreateTime");
                            java.util.Date createTime = (java.util.Date) getCreateTime.invoke(latest);
                            if (createTime != null) {
                                activity.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createTime));
                            } else {
                                activity.put("time", LocalDateTime.now().format(formatter));
                            }
                            activities.add(activity);
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取实习记录失败", e);
            }
            
            // 按时间排序，取最新的5条（首页显示）
            activities.sort((a, b) -> {
                String timeA = (String) a.get("time");
                String timeB = (String) b.get("time");
                return timeB.compareTo(timeA);
            });
            if (activities.size() > 5) {
                activities = activities.subList(0, 5);
            }
            
            result.put("recentActivities", activities);
            
            // 系统信息
            Map<String, Object> systemInfo = new HashMap<>();
            try {
                // 获取在线用户数
                int onlineUsers = onlineUserService.getOnlineUserCount();
                systemInfo.put("onlineUsers", onlineUsers);
            } catch (Exception e) {
                log.warn("获取在线用户数失败", e);
                systemInfo.put("onlineUsers", 0);
            }
            
            try {
                // 获取今日访问次数
                long todayVisits = visitStatisticsService.getTodayVisitCount();
                systemInfo.put("todayVisits", todayVisits);
            } catch (Exception e) {
                log.warn("获取今日访问次数失败", e);
                systemInfo.put("todayVisits", 0);
            }
            
            systemInfo.put("uptime", "系统运行中");
            systemInfo.put("dbStatus", "正常");
            result.put("systemInfo", systemInfo);
            
            // 待审批事项 (聚合所有待审核的记录)
            List<Map<String, Object>> pendingApprovals = new ArrayList<>();
            
            // 1. 学术活动待审核
            try {
                List<AcademicEventModel> pendingAcademic = academicEventRepository.findByAuditStatus("pending");
                for (AcademicEventModel item : pendingAcademic.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "academic_" + item.getId());
                    approval.put("type", "学术活动");
                    approval.put("title", "学术活动：" + (item.getTitle() != null ? item.getTitle() : ""));
                    approval.put("description", "待审核的学术活动记录");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "academic");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取学术活动待审核失败", e);
            }
            
            // 2. 日常活动待审核
            try {
                List<DailyActivityModel> pendingDaily = dailyActivityRepository.findByAuditStatus("pending");
                for (DailyActivityModel item : pendingDaily.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "daily_" + item.getId());
                    approval.put("type", "日常活动");
                    approval.put("title", "日常活动：" + (item.getTitle() != null ? item.getTitle() : ""));
                    approval.put("description", "待审核的日常活动记录");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "daily");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取日常活动待审核失败", e);
            }
            
            // 3. 荣誉待审核
            try {
                List<HonorModel> pendingHonors = honorRepository.findByAuditStatus("pending");
                for (HonorModel item : pendingHonors.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "honor_" + item.getId());
                    approval.put("type", "荣誉");
                    approval.put("title", "荣誉：" + (item.getTitle() != null ? item.getTitle() : ""));
                    approval.put("description", "待审核的荣誉记录");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "honor");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取荣誉待审核失败", e);
            }
            
            // 4. 思想汇报待审核
            try {
                List<ThoughtReportModel> pendingReports = thoughtReportService.getPendingThoughtReports();
                for (ThoughtReportModel item : pendingReports.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "report_" + item.getId());
                    approval.put("type", "思想汇报");
                    approval.put("title", "思想汇报：" + (item.getTitle() != null ? item.getTitle() : ""));
                    approval.put("description", "待审核的思想汇报");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "thoughtReport");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取思想汇报待审核失败", e);
            }
            
            // 5. 微党课待审核
            try {
                List<PartyCourseModel> pendingCourses = partyCourseService.getPendingPartyCourses();
                for (PartyCourseModel item : pendingCourses.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "course_" + item.getId());
                    approval.put("type", "微党课");
                    approval.put("title", "微党课：" + (item.getTitle() != null ? item.getTitle() : ""));
                    approval.put("description", "待审核的微党课记录");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "partyCourse");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取微党课待审核失败", e);
            }
            
            // 6. 志愿服务待审核
            try {
                List<VolunteerServiceModel> pendingServices = volunteerServiceService.getPendingVolunteerServices();
                for (VolunteerServiceModel item : pendingServices.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "service_" + item.getId());
                    approval.put("type", "志愿服务");
                    approval.put("title", "志愿服务：" + (item.getServiceName() != null ? item.getServiceName() : ""));
                    approval.put("description", "待审核的志愿服务记录");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "volunteerService");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取志愿服务待审核失败", e);
            }
            
            // 7. 入党申请待审核
            try {
                List<PartyApplicationModel> pendingApplications = partyApplicationRepository.findByAuditStatus("待审核");
                for (PartyApplicationModel item : pendingApplications.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "application_" + item.getId());
                    approval.put("type", "入党申请");
                    approval.put("title", "入党申请：" + (item.getName() != null ? item.getName() : ""));
                    approval.put("description", "待审核的入党申请");
                    approval.put("time", item.getApplicationDate() != null ? item.getApplicationDate() : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "partyApplication");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取入党申请待审核失败", e);
            }
            
            // 8. 请假待审核
            try {
                List<LeaveRequestModel> pendingLeaves = leaveRequestRepository.findByAuditStatus("pending");
                for (LeaveRequestModel item : pendingLeaves.stream().limit(5).collect(Collectors.toList())) {
                    Map<String, Object> approval = new HashMap<>();
                    approval.put("id", "leave_" + item.getId());
                    approval.put("type", "请假申请");
                    approval.put("title", "请假申请：" + (item.getStudentName() != null ? item.getStudentName() : "") + " - " + (item.getReason() != null ? item.getReason() : ""));
                    approval.put("description", "待审核的请假申请");
                    approval.put("time", item.getCreateTime() != null ? item.getCreateTime().format(formatter) : LocalDateTime.now().format(formatter));
                    approval.put("dataId", item.getId());
                    approval.put("dataType", "leave");
                    pendingApprovals.add(approval);
                }
            } catch (Exception e) {
                log.warn("获取请假待审核失败", e);
            }
            
            // 按时间排序，取最新的10条
            pendingApprovals.sort((a, b) -> {
                String timeA = (String) a.get("time");
                String timeB = (String) b.get("time");
                return timeB.compareTo(timeA);
            });
            if (pendingApprovals.size() > 10) {
                pendingApprovals = pendingApprovals.subList(0, 10);
            }
            
            result.put("pendingApprovals", pendingApprovals);

            result.put("code", 200);
        } catch (Exception e) {
            log.error("获取仪表盘统计失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取待审核统计
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getAuditStats() {
        try {
            log.info("=== 获取待审核统计数据 ===");
            
            Map<String, Object> stats = new HashMap<>();

            // 竞赛统计
            Map<String, Integer> competitionStats = new HashMap<>();
            competitionStats.put("pending", (int) competitionService.countByAuditStatus("待审核"));
            competitionStats.put("approved", (int) competitionService.countByAuditStatus("已通过"));
            competitionStats.put("rejected", (int) competitionService.countByAuditStatus("已驳回"));
            stats.put("competition", competitionStats);

            // 项目统计
            Map<String, Integer> projectStats = new HashMap<>();
            projectStats.put("pending", (int) projectService.countByAuditStatus("待审核"));
            projectStats.put("approved", (int) projectService.countByAuditStatus("已通过"));
            projectStats.put("rejected", (int) projectService.countByAuditStatus("已驳回"));
            stats.put("project", projectStats);

            // 论文统计
            Map<String, Integer> paperStats = new HashMap<>();
            paperStats.put("pending", (int) paperService.countByAuditStatus("待审核"));
            paperStats.put("approved", (int) paperService.countByAuditStatus("已通过"));
            paperStats.put("rejected", (int) paperService.countByAuditStatus("已驳回"));
            stats.put("paper", paperStats);

            // 专利统计
            Map<String, Integer> patentStats = new HashMap<>();
            patentStats.put("pending", (int) patentService.countByAuditStatus("待审核"));
            patentStats.put("approved", (int) patentService.countByAuditStatus("已通过"));
            patentStats.put("rejected", (int) patentService.countByAuditStatus("已驳回"));
            stats.put("patent", patentStats);

            // 日常管理统计（包括请假、学术活动、日常活动、荣誉）
            Map<String, Integer> dailyStats = new HashMap<>();
            try {
                // 请假待审核
                int leavePending = leaveRequestRepository.findByAuditStatus("pending").size();
                // 学术活动待审核
                int academicPending = academicEventRepository.findByAuditStatus("pending").size();
                // 日常活动待审核
                int dailyActivityPending = dailyActivityRepository.findByAuditStatus("pending").size();
                // 荣誉待审核
                int honorPending = honorRepository.findByAuditStatus("pending").size();
                
                dailyStats.put("pending", leavePending + academicPending + dailyActivityPending + honorPending);
                dailyStats.put("leave", leavePending);
                dailyStats.put("academic", academicPending);
                dailyStats.put("dailyActivity", dailyActivityPending);
                dailyStats.put("honor", honorPending);
            } catch (Exception e) {
                log.warn("获取日常管理待审核统计失败", e);
                dailyStats.put("pending", 0);
                dailyStats.put("leave", 0);
                dailyStats.put("academic", 0);
                dailyStats.put("dailyActivity", 0);
                dailyStats.put("honor", 0);
            }
            stats.put("daily", dailyStats);

            log.info("统计结果: {}", stats);
            return ResponseEntity.ok(stats);  // ✅ 直接返回 Map
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ResponseEntity.badRequest().body("获取统计数据失败: " + e.getMessage());
        }
    }
}
