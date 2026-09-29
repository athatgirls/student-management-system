package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "students")
public class StudentModel {
    @Id
    private String id;
    private String studentId; // 学号
    private String name; // 姓名
    private String gender; // 性别
    private Integer age; // 年龄
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password; // 密码（BCrypt 加密存储，禁止通过 JSON 返回）
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Boolean passwordChangeRequired;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Date initialPasswordExpiresAt;
    private String maritalStatus; // 婚姻状况
    private String nation; // 民族
    private Date birthDate; // 出生日期
    private String idCard; // 身份证号
    private String educationType; // 学制类型（如全日制、非全日制等）
    private String email; // 邮箱
    private String phone; // 手机号
    private String major; // 专业
    private String grade; // 年级/入学年份
    private String className; // 班级
    private String supervisor; // 导师姓名
    private String researchDirection; // 研究方向
    private String status; // 学籍状态（在读/毕业/休学等）
    private String statusRemark; // 状态备注（非在读状态需要填写）
    private Date createTime; // 注册时间
    private Date updateTime; // 信息更新时间
    private String politicalStatus; // 政治面貌
    private String address; // 现住址
    private String dormitory; // 宿舍信息
    private String emergencyContact; // 紧急联系人
    private String nativePlace; // 籍贯
    private String workStatus; // 任职情况（如班委、学生组织、助管等）
    private String introduction; // 个人简介

    // 手动添加 Getter 和 Setter 以确保即使 Lombok 出现缓存问题也能编译
    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }
}
