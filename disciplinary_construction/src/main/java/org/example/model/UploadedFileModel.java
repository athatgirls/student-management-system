package org.example.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Document(collection = "uploaded_files")
public class UploadedFileModel {
    @Id private String filename;
    /** 附件归属：已认证学生的账号主键，即 studentAccountId，可与业务记录直接比较 */
    private String ownerId;
    private long size;
}
