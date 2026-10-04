package org.example.controller;

import org.example.response.ResponseResult;
import org.example.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/msi/upload")
public class FileUploadController {
    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
    @org.springframework.beans.factory.annotation.Autowired
    private org.example.repository.UploadedFileRepository uploadedFiles;

    @Autowired
    private ObjectStorageService objectStorage;

    private void recordOwner(String filename, MultipartFile file, HttpServletRequest request) {
        Object owner = request.getAttribute("userId");
        if (!(owner instanceof String)) throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        org.example.model.UploadedFileModel record = new org.example.model.UploadedFileModel();
        record.setFilename(filename); record.setOwnerId((String) owner); record.setSize(file.getSize());
        uploadedFiles.insert(record);
    }

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final int MAX_FILES_PER_REQUEST = 5;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".pdf", ".doc", ".docx"
    ));

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostMapping
    public ResponseResult<List<String>> uploadFiles(@RequestParam("file") MultipartFile[] files, HttpServletRequest request) {
        List<String> fileUrls = new ArrayList<>();

        if (files == null || files.length == 0 || files.length > MAX_FILES_PER_REQUEST) {
            return new ResponseResult<>(400, "每次请上传 1 至 5 个文件", null);
        }

        try {
            String baseUrl = getBaseUrl(request);

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String validationError = validateFile(file);
                    if (validationError != null) {
                        return new ResponseResult<>(400, validationError, null);
                    }
                }
            }

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String filename = buildSafeFilename(file.getOriginalFilename());
                    storeAndRecord(filename, file, request);
                    fileUrls.add(baseUrl + "/uploads/" + filename);
                }
            }

            return ResponseResult.success(fileUrls);
        } catch (IOException e) {
            log.error("Batch file upload failed", e);
            return new ResponseResult<>(500, "文件上传失败", null);
        }
    }

    @PostMapping("/single")
    public ResponseResult<String> uploadSingleFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            if (file.isEmpty()) {
                return new ResponseResult<>(400, "File cannot be empty", null);
            }

            String validationError = validateFile(file);
            if (validationError != null) {
                return new ResponseResult<>(400, validationError, null);
            }

            String filename = buildSafeFilename(file.getOriginalFilename());
            storeAndRecord(filename, file, request);
            return ResponseResult.success(getBaseUrl(request) + "/uploads/" + filename);
        } catch (IOException e) {
            log.error("Single file upload failed", e);
            return new ResponseResult<>(500, "文件上传失败", null);
        }
    }

    private void storeAndRecord(String filename, MultipartFile file, HttpServletRequest request) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            objectStorage.putObject(filename, inputStream, file.getSize(), file.getContentType());
        }
        try {
            recordOwner(filename, file, request);
        } catch (RuntimeException e) {
            log.error("File ownership persistence failed for key {}; deleting stored object", filename, e);
            try {
                objectStorage.deleteObject(filename);
            } catch (IOException deleteFailure) {
                log.error("Compensating deletion failed for key {}", filename, deleteFailure);
            }
            throw e;
        }
    }

    private String validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            return "单个文件不能超过 10MB";
        }
        String extension = getSafeExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "仅支持 JPG、PNG、PDF、DOC、DOCX 文件";
        }
        try (InputStream stream = file.getInputStream()) {
            byte[] header = stream.readNBytes(8);
            StringBuilder hex = new StringBuilder();
            for (byte b : header) hex.append(String.format("%02x", b & 0xff));
            String signature = hex.toString();
            boolean valid = ((extension.equals(".jpg") || extension.equals(".jpeg")) && signature.startsWith("ffd8ff"))
                    || (extension.equals(".png") && signature.equals("89504e470d0a1a0a"))
                    || (extension.equals(".pdf") && signature.startsWith("255044462d"))
                    || (extension.equals(".doc") && signature.equals("d0cf11e0a1b11ae1"))
                    || (extension.equals(".docx") && signature.startsWith("504b0304"));
            if (!valid) return "文件内容与扩展名不匹配";
        } catch (IOException e) {
            return "无法读取文件内容";
        }
        return null;
    }

    private String getBaseUrl(HttpServletRequest request) {
        if (contextPath != null && !contextPath.isEmpty()) {
            return contextPath;
        }
        String requestContextPath = request.getContextPath();
        return requestContextPath != null && !requestContextPath.isEmpty() ? requestContextPath : "";
    }

    private String buildSafeFilename(String originalFilename) {
        String extension = getSafeExtension(originalFilename);
        return UUID.randomUUID() + extension;
    }

    private String getSafeExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String name = Paths.get(originalFilename).getFileName().toString();
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        String extension = name.substring(index).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,12}") ? extension : "";
    }
}
