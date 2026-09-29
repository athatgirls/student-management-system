package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "majors")
public class MajorModel {
    @Id
    private String id;
    private String majorName;
    private String description;
    private Date createTime;
    private Date updateTime;
    private String createBy;
    private String remark;
}
