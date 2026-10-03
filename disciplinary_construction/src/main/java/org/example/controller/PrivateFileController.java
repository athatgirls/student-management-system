package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.UploadedFileModel;
import org.example.repository.UploadedFileRepository;
import org.example.service.CurrentUserAccessService;
import org.example.service.ObjectStorageService;
import org.example.service.ObjectStorageNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
import java.io.IOException;

@RestController
public class PrivateFileController {
    private final UploadedFileRepository files;
    private final CurrentUserAccessService access;
    private final ObjectStorageService objectStorage;
    public PrivateFileController(UploadedFileRepository files, CurrentUserAccessService access, ObjectStorageService objectStorage) {
        this.files = files; this.access = access; this.objectStorage = objectStorage;
    }
    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename, @CurrentUser Map<String,Object> user) throws IOException {
        if (!filename.matches("[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|pdf|doc|docx)")) return ResponseEntity.notFound().build();
        UploadedFileModel metadata = files.findById(filename).orElse(null);
        // Legacy files have no provable ownership: admin-only until explicitly migrated.
        if (!access.isAdmin(user) && (metadata == null || !access.requireUserId(user).equals(metadata.getOwnerId())))
            throw new AccessDeniedException("无权访问该材料，请联系管理员核实历史文件归属");
        Resource object;
        try {
            object = new InputStreamResource(objectStorage.getObject(filename));
        } catch (ObjectStorageNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        boolean image = filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg");
        MediaType type = image ? (filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG) : MediaType.APPLICATION_OCTET_STREAM;
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .header(HttpHeaders.CONTENT_DISPOSITION, (image ? "inline" : "attachment") + "; filename=\"" + filename + "\"")
                .contentType(type);
        if (metadata != null) {
            response.contentLength(metadata.getSize());
        }
        return response.body(object);
    }
}
