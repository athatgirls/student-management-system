package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "grades")
public class GradeModel {
    @Id
    private String id;
    private String gradeName;
    private String description;
    private Date createTime;
    private Date updateTime;
    private String createBy;
    private String remark;
}
