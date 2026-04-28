package org.example.dto;

import lombok.Data;

@Data
public class StudyRecordImportItem {
    private String studentId;
    private String studentName;
    private String semester;
    private String courseName;
    private Object score;
    private Object credit;
    private String status;
}
