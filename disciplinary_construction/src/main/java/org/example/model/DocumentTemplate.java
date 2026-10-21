package org.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "document_templates")
public class DocumentTemplate {
    @Id private String id;
    private String title;
    private String description;
    private String originalName;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String objectKey;
    private long size;
    private LocalDateTime createTime;
    private boolean deleting;
}
