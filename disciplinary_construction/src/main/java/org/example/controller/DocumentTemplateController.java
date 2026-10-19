package org.example.controller;

import org.example.model.DocumentTemplate;
import org.example.service.DocumentTemplateService;
import org.example.response.ResponseResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/msi/templates")
public class DocumentTemplateController {
    private final DocumentTemplateService service;
    public DocumentTemplateController(DocumentTemplateService service) { this.service = service; }

    @GetMapping("/list")
    public ResponseResult<List<DocumentTemplate>> list() { return ResponseResult.success(service.list()); }

    @GetMapping("/admin/list")
    public ResponseResult<List<DocumentTemplate>> adminList() { return ResponseResult.success(service.adminList()); }

    @PostMapping("/admin/upload")
    public ResponseResult<DocumentTemplate> upload(@RequestParam String title,
            @RequestParam(defaultValue = "") String description, @RequestParam MultipartFile file) throws IOException {
        return ResponseResult.success(service.upload(title, description, file));
    }

    @PostMapping("/admin/{id}/delete")
    public ResponseResult<String> delete(@PathVariable String id) throws IOException {
        service.delete(id);
        return ResponseResult.success("模板已删除");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) throws IOException {
        DocumentTemplate template = service.find(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(template.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(service.download(template)));
    }
}
