package org.example.controller;

import org.example.response.ResponseResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/msi/upload")
public class FileUploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostMapping
    public ResponseResult<List<String>> uploadFiles(@RequestParam("file") MultipartFile[] files, HttpServletRequest request) {
        List<String> fileUrls = new ArrayList<>();

        try {
            Path uploadPath = getUploadPath();
            Files.createDirectories(uploadPath);
            String baseUrl = getBaseUrl(request);

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String filename = buildSafeFilename(file.getOriginalFilename());
                    Path filePath = uploadPath.resolve(filename).normalize();
                    if (!filePath.startsWith(uploadPath)) {
                        return new ResponseResult<>(400, "Invalid file path", null);
                    }
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    fileUrls.add(baseUrl + "/uploads/" + filename);
                }
            }

            return ResponseResult.success(fileUrls);
        } catch (IOException e) {
            return new ResponseResult<>(500, "File upload failed: " + e.getMessage(), null);
        }
    }

    @PostMapping("/single")
    public ResponseResult<String> uploadSingleFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            if (file.isEmpty()) {
                return new ResponseResult<>(400, "File cannot be empty", null);
            }

            Path uploadPath = getUploadPath();
            Files.createDirectories(uploadPath);

            String filename = buildSafeFilename(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return new ResponseResult<>(400, "Invalid file path", null);
            }
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseResult.success(getBaseUrl(request) + "/uploads/" + filename);
        } catch (IOException e) {
            return new ResponseResult<>(500, "File upload failed: " + e.getMessage(), null);
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        if (contextPath != null && !contextPath.isEmpty()) {
            return contextPath;
        }
        String requestContextPath = request.getContextPath();
        return requestContextPath != null && !requestContextPath.isEmpty() ? requestContextPath : "";
    }

    private Path getUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
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
