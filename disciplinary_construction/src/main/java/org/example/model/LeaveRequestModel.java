package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "leave_requests")
public class LeaveRequestModel {
    @Id
    private String id;

    // 学生基本信息（自动从学生表获取）
    private String studentId;       // 学号（学生ID）
    private String studentName;     // 学生姓名
    private String grade;           // 年级
    private String major;            // 专业
    private String className;        // 班级
    private String phone;            // 电话号

    // 请假信息
    private String reason;          // 请假原因
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;    // 请假开始时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;      // 请假结束时间
    private Integer days;           // 请假天数（自动计算）
    private String status;          // 请假状态：pending（待审核）、approved（已批准）、rejected（已拒绝）、on_leave（假期中）、pending_check_in（待销假）、completed（已销假）、overdue（逾期未销假）

    // 证明材料（图片URL列表）
    private List<String> evidenceUrls;  // 请假证据图片URL列表

    // 审核信息
    private String auditStatus;     // 审核状态（pending/approved/rejected）
    private String auditComment;    // 审核意见
    private String auditorId;       // 审核人ID
    private String auditorName;     // 审核人姓名
    private LocalDateTime auditTime;// 审核时间

    // 销假信息
    private LocalDateTime checkInTime;  // 销假时间
    private String checkInComment;     // 销假备注
    private Double checkInLatitude;    // 销假定位：纬度
    private Double checkInLongitude;   // 销假定位：经度
    private String checkInAddress;     // 销假定位：地址

    // 元信息
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime updateTime;   // 更新时间
}

