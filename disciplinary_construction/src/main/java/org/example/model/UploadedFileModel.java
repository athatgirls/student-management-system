package org.example.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Document(collection = "uploaded_files")
public class UploadedFileModel {
    @Id private String filename;
    private String ownerId;
    private long size;
}
